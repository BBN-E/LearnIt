package com.bbn.akbc.neolearnit.server.handlers;

import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

public abstract class SimpleJSONHandler extends AbstractHandler {

	protected <T> T loadObject(String json, Class<T> type) throws IOException {
		ObjectMapper mapper = StorageUtils.getMapperWithoutTyping();
		return mapper.readValue(json, type);
	}

	@Override
	public void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException {

		for (Method m : this.getClass().getMethods()) {
			JettyMethod jm = m.getAnnotation(JettyMethod.class);

//			if(jm!=null) {
//				System.out.println("DBG:\t"+jm.value());
//				System.out.println("DBG:\t"+target);
//			}
			if (jm != null && target.equals(jm.value())) {
				List<Object> params = new ArrayList<Object>();
				for (Annotation[] paramAnns : m.getParameterAnnotations()) {
					for (Annotation ann : paramAnns) {
						if (ann instanceof JettyArg) {
							String name = ((JettyArg) ann).value();
							String strVal = baseRequest.getParameter(name);
							Object val = null;
							if (strVal != null) {
								//val = JSONValue.parse(baseRequest.getParameter(name));
								//if (val == null) {
								val = strVal;
								//}
							} else {
								String[] vals = baseRequest.getParameterValues(name+"[]");
								if (vals != null) {
									val = vals;
								} else {
									throw new RuntimeException("Missing argument "+name+" for method "+jm.value()+", params: "+baseRequest.getParameterMap().keySet());
								}
							}

							params.add(val);
							break;
						}
					}
				}

				try {
					System.out.println("==========================================================");
					System.out.println("    Calling "+m.getName()+" with args "+params);
					System.out.println("==========================================================");

					Object result = m.invoke(this, params.toArray());
					ObjectMapper mapper = StorageUtils.getMapperWithoutTyping();

					response.setContentType("application/json;charset=utf-8");
					response.setStatus(HttpServletResponse.SC_OK);
					response.addHeader("Access-Control-Allow-Origin", "*");
					baseRequest.setHandled(true);
					mapper.writeValue(response.getWriter(), result);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}

	}


	@Target({ElementType.METHOD})
	@Retention(RUNTIME)
	protected @interface JettyMethod {
		String value();
	}

	@Target({ElementType.PARAMETER})
	@Retention(RUNTIME)
	protected @interface JettyArg {
		String value();
	}

}
