package com.bbn.akbc.neolearnit.storage;

import com.bbn.akbc.neolearnit.storage.impl.HashMapStorage;
import com.bbn.akbc.utility.Pair;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class StorageUtils {

	public static ObjectMapper getDefaultMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
		mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.NONE)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE));
		mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
//		mapper.registerModule(new BUECommonModule());
        mapper.findAndRegisterModules();
		return mapper;
	}

	public static ObjectMapper getMapperWithoutTyping() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.NONE)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE));
//		mapper.registerModule(new BUECommonModule());
        mapper.findAndRegisterModules();
		return mapper;
	}

//	@Deprecated
//	public static ObjectMapper getSmileMapper() {
//		ObjectMapper mapper = new ObjectMapper(new SmileFactory());
//		mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
//		mapper.registerModule(new BUECommonModule());
//		return mapper;
//	}

	@SuppressWarnings({ "unchecked" })
	public static <T,U> MapStorage<T,U> deserializeMapStorage(File file, boolean compress) throws IOException {
		if (compress) {
			InputStream in = new InflaterInputStream(new FileInputStream(file));
			return StorageUtils.getDefaultMapper().readValue(in, MapStorage.class);
		} else {
			return StorageUtils.getDefaultMapper().readValue(new FileInputStream(file), MapStorage.class);
		}
	}

	public static <T,U> void serializeMapStorage(File file, MapStorage<T,U> mapStorage, boolean compress) throws IOException {
		if (mapStorage instanceof HashMapStorage) {
			OutputStream out;
			if (compress) {
				out = new DeflaterOutputStream(new FileOutputStream(file));
			} else {
				out = new FileOutputStream(file);
			}
			getDefaultMapper().writeValue(out, mapStorage);
			out.close();
		} else {
			throw new RuntimeException("Unsupported map storage serialization type.");
		}
	}

	public static <T> T deserialize(File file, Class<T> cls, boolean compress) throws IOException {
		if (compress) {
			InputStream in = new InflaterInputStream(new FileInputStream(file));
			return StorageUtils.getDefaultMapper().readValue(in, cls);
		} else {
            InputStream in = new FileInputStream(file);
			return StorageUtils.getDefaultMapper().readValue(in, cls);
		}
	}

	public static <T> void serialize(File file, T obj, boolean compress) throws IOException {
		OutputStream out;
		if (compress) {
			out = new DeflaterOutputStream(new FileOutputStream(file));
		} else {
			out = new FileOutputStream(file);
		}
		getDefaultMapper().writeValue(out, obj);
		out.close();
	}

  public static <T,U> Set<Pair<T,U>> convertMapToSerializablePairs(Map<T,U> map) {
    Set<Pair<T,U>> result = new HashSet<Pair<T,U>>();
    for (Map.Entry<T,U> entry : map.entrySet()) {
      result.add(Pair.fromEntry(entry));
    }
    return result;
  }
}
