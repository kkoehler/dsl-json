package com.dslplatform.json;

import com.dslplatform.json.runtime.Settings;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class EnumTest {

	@CompiledJson(onUnknown = CompiledJson.Behavior.IGNORE)
	public enum MyEnum1 {
		ABC,
		DEF,
		GHI;
	}

	public enum MyEnum2 {
		XX1,
		YY,
		ZZ2;
	}

	@CompiledJson
	public enum DuplicateHash {
		n3307663,
		n519524;
	}

	@CompiledJson
	public static class SingleNonImmutable {
		public MyEnum1 e1;
		private MyEnum2 e2;
		@JsonAttribute(nullable = false)
		public MyEnum1 e3;

		public MyEnum2 e2() {
			return e2;
		}

		public void e2(MyEnum2 v) {
			this.e2 = v;
		}

		public Map<MyEnum1, Integer> map1;
		public List<MyEnum2> list2;
	}

	@CompiledJson
	public static class SingleImmutable {
		public final MyEnum1 e1;
		public final MyEnum2 e2;
		@JsonAttribute(nullable = false)
		public MyEnum1 e3;
		public final Map<MyEnum1, Integer> map1;
		private List<MyEnum2> list2;

		public List<MyEnum2> list2() {
			return list2;
		}

		public SingleImmutable(MyEnum1 e1, MyEnum2 e2, MyEnum1 e3, Map<MyEnum1, Integer> map1, List<MyEnum2> list2) {
			this.e1 = e1;
			this.e2 = e2;
			this.e3 = e3;
			this.map1 = map1;
			this.list2 = list2;
		}
	}

	// Test @JsonValue annotation on field
	@CompiledJson(onUnknown = CompiledJson.Behavior.IGNORE)
	public enum EnumWithCustomNames1 {
		TEST_A1("a1"),
		TEST_A2("a2"),
		TEST_A3("a3");

		@JsonValue
		public final String value;

		EnumWithCustomNames1(String value) {
			this.value = value;
		}
	}

	// Test @JsonValue annotation on method
	@CompiledJson
	public enum EnumWithCustomNames2 {
		TEST_B1("b1"),
		TEST_B2("b2"),
		TEST_B3("b3");

		private final String value;

		EnumWithCustomNames2(String value) {
			this.value = value;
		}

		@JsonValue
		public String getValue() {
			return value;
		}
	}

	// Test @JsonValue annotation with 'int' type
	public enum EnumWithCustomNames3 {
		TEST_C1,
		TEST_C2,
		TEST_C3;

		@JsonValue
		public int getValue() {
			switch (this) {
				case TEST_C1: return 10;
				case TEST_C2: return 20;
				case TEST_C3: return 30;
			}
			throw new IllegalStateException();
		}
	}

	@CompiledJson
	public static class EnumHolder {
		public EnumWithCustomNames1 enum1;
		public EnumWithCustomNames2 enum2;
		public EnumWithCustomNames3 enum3;
		public List<EnumWithCustomNames1> enumList1;
		public List<EnumWithCustomNames2> enumList2;
		public List<EnumWithCustomNames3> enumList3;
	}

	private final DslJson<Object> dslJson = new DslJson<>(Settings.withRuntime().includeServiceLoader());

	@Test
	public void objectRoundtrip() throws IOException {
		SingleNonImmutable sni = new SingleNonImmutable();
		sni.e1 = MyEnum1.DEF;
		sni.e2(MyEnum2.ZZ2);
		sni.e3 = MyEnum1.GHI;
		sni.map1 = new LinkedHashMap<>();
		sni.map1.put(MyEnum1.ABC, 2);
		sni.map1.put(MyEnum1.GHI, 5);
		sni.list2 = Arrays.asList(MyEnum2.ZZ2, MyEnum2.YY);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		dslJson.serialize(sni, os);
		SingleNonImmutable res = dslJson.deserialize(SingleNonImmutable.class, os.toByteArray(), os.size());
		Assert.assertEquals(sni.e1, res.e1);
		Assert.assertEquals(sni.e2, res.e2);
		Assert.assertEquals(sni.e3, res.e3);
		Assert.assertEquals(sni.map1, res.map1);
		Assert.assertEquals(sni.list2, res.list2);
	}

	@Test
	public void immutableRoundtrip() throws IOException {
		Map map1 = new LinkedHashMap<>();
		map1.put(MyEnum1.ABC, 2);
		map1.put(MyEnum1.GHI, 5);
		SingleImmutable si = new SingleImmutable(
				MyEnum1.DEF,
				MyEnum2.ZZ2,
				MyEnum1.GHI,
				map1,
				Arrays.asList(MyEnum2.ZZ2, MyEnum2.YY));
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		dslJson.serialize(si, os);
		SingleImmutable res = dslJson.deserialize(SingleImmutable.class, os.toByteArray(), os.size());
		Assert.assertEquals(si.e1, res.e1);
		Assert.assertEquals(si.e2, res.e2);
		Assert.assertEquals(si.e3, res.e3);
		Assert.assertEquals(si.map1, res.map1);
		Assert.assertEquals(si.list2, res.list2);
	}

	@Test
	public void errorOnUnknown() throws IOException {
		byte[] json = "{\"e2\":\"A\"}".getBytes("UTF-8");
		try {
			dslJson.deserialize(SingleNonImmutable.class, json, json.length);
			Assert.fail("Exception expected");
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(e.getMessage().contains("No enum constant com.dslplatform.json.EnumTest.MyEnum2.A"));
		}
	}

	@Test
	public void defaultOnUnknown() throws IOException {
		byte[] json = "{\"e1\":\"A\"}".getBytes("UTF-8");
		SingleNonImmutable v = dslJson.deserialize(SingleNonImmutable.class, json, json.length);
		Assert.assertEquals(MyEnum1.ABC, v.e1);
	}

	@Test
	public void testCustomNames() throws IOException {
		EnumHolder model = new EnumHolder();
		model.enum1 = EnumWithCustomNames1.TEST_A1;
		model.enum2 = EnumWithCustomNames2.TEST_B2;
		model.enum3 = EnumWithCustomNames3.TEST_C3;
		model.enumList1 = Arrays.asList(EnumWithCustomNames1.values());
		model.enumList2 = Arrays.asList(EnumWithCustomNames2.values());
		model.enumList3 = Arrays.asList(EnumWithCustomNames3.values());

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		dslJson.serialize(model, os);
		byte[] json = os.toByteArray();

		Assertions.assertThat(new String(json))
				.isEqualTo("{\"enumList3\":[10,20,30],\"enumList2\":[\"b1\",\"b2\",\"b3\"],\"enumList1\":[\"a1\",\"a2\",\"a3\"],\"enum1\":\"a1\",\"enum2\":\"b2\",\"enum3\":30}");

		EnumHolder result = dslJson.deserialize(EnumHolder.class, json, json.length);
		Assertions.assertThat(result).isEqualToComparingFieldByFieldRecursively(model);
	}

	@Test
	public void defaultOnUnknown_customNames() throws IOException {
		byte[] json = "[\"Z\"]".getBytes(StandardCharsets.UTF_8);
		List<EnumWithCustomNames1> result = dslJson.deserializeList(EnumWithCustomNames1.class, json, json.length);
		Assertions.assertThat(result).containsExactly(EnumWithCustomNames1.TEST_A1);
	}

	@Test
	public void errorOnUnknown_customNames() {
		byte[] json = "[\"Z\"]".getBytes(StandardCharsets.UTF_8);

		Assertions.assertThatThrownBy(() ->
				dslJson.deserializeList(EnumWithCustomNames2.class, json, json.length)
		).hasMessage("No enum constant com.dslplatform.json.EnumTest.EnumWithCustomNames2 associated with value 'Z'");
	}
}