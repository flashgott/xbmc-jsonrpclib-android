package org.xbmc.android.jsonrpc.generator;

import java.io.File;
import java.io.IOException;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.module.SimpleModule;
import org.xbmc.android.jsonrpc.generator.controller.PropertyController;
import org.xbmc.android.jsonrpc.generator.introspect.Property;
import org.xbmc.android.jsonrpc.generator.introspect.Response;
import org.xbmc.android.jsonrpc.generator.introspect.Result;
import org.xbmc.android.jsonrpc.generator.introspect.Type;
import org.xbmc.android.jsonrpc.generator.introspect.wrapper.AdditionalPropertiesWrapper;
import org.xbmc.android.jsonrpc.generator.introspect.wrapper.ExtendsWrapper;
import org.xbmc.android.jsonrpc.generator.introspect.wrapper.TypeWrapper;
import org.xbmc.android.jsonrpc.generator.model.Namespace;
import org.xbmc.android.jsonrpc.generator.view.NamespaceView;
import org.xbmc.android.jsonrpc.jackson.AdditionalPropertiesDeserializer;
import org.xbmc.android.jsonrpc.jackson.ExtendsDeserializer;
import org.xbmc.android.jsonrpc.jackson.TypeDeserializer;

public class Introspect {
	
	public final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static Result RESULT;
	
	static {
		final SimpleModule module = new SimpleModule("", Version.unknownVersion());
		module.addDeserializer(TypeWrapper.class, new TypeDeserializer());
		module.addDeserializer(ExtendsWrapper.class, new ExtendsDeserializer());
		module.addDeserializer(AdditionalPropertiesWrapper.class, new AdditionalPropertiesDeserializer());
		
		OBJECT_MAPPER.registerModule(module);
	}
	
	public static void main(String[] args) {
		try {
			
		    final Response response = OBJECT_MAPPER.readValue(new File("introspect.json"), Response.class);
		    RESULT = response.getResult();
			
		    for (String name : RESULT.getTypes().keySet()) {
		    	final PropertyController controller = new PropertyController(name, RESULT.getTypes().get(name));
		    	controller.register();
		    }
		    
		    for (Namespace ns : Namespace.getAll()) {
		    	final NamespaceView view = new NamespaceView(ns);
		    	System.out.print(view.render());
		    }
		    
			System.out.println("Done!");
			
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static Property find(Property property) {
		if (RESULT == null) {
			throw new RuntimeException("Must parse before finding types!");
		}
		if (property.isRef()) {
			final Type type = RESULT.getTypes().get(property.getRef());
			if (!RESULT.getTypes().containsKey(property.getRef())) {
				throw new RuntimeException("Cannot find type " + property.getRef() + ".");
			}
			return type;
		} else {
			return property;
		}
	}
}