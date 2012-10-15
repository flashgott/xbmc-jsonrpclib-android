/*
 *      Copyright (C) 2005-2012 Team XBMC
 *      http://xbmc.org
 *
 *  This Program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2, or (at your option)
 *  any later version.
 *
 *  This Program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with XBMC Remote; see the file license.  If not, write to
 *  the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *  http://www.gnu.org/copyleft/gpl.html
 *
 */
package org.xbmc.android.jsonrpc.generator.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xbmc.android.jsonrpc.generator.Introspect;
import org.xbmc.android.jsonrpc.generator.view.module.IClassModule;

/**
 * Defines a class in an agnostic way.
 * 
 * @author freezy <freezy@xbmc.org>
 */
public class Klass {

	private final String name;
	private final String apiType;
	private final Namespace namespace;

	private boolean isNative = false;
	private boolean isInner = false;
	private boolean isMultiType = false;
	private boolean isArray = false;
	private boolean isGlobal = false;
	

	/**
	 * If true, this is just a place holder and the "real" object has yet to be
	 * fetched.
	 */
	private final boolean unresolved;

	private final List<Constructor> constructors = new ArrayList<Constructor>();
	private final List<Member> members = new ArrayList<Member>();
	private final List<Klass> innerTypes = new ArrayList<Klass>();
	private final List<Enum> innerEnums = new ArrayList<Enum>();

	private final Set<String> imports = new HashSet<String>();
	private final static Map<String, Klass> GLOBALS = new HashMap<String, Klass>();

	private Klass parentClass = null; // set if "extends" 
	private Klass arrayType = null;
	private Klass outerType = null; // set if isInner == true.

	/**
	 * New class by reference.
	 * 
	 * Only the "id" of the global type is provided. When rendering the class
	 * later, it must be resolved by using {@link #resolve(Klass)}.
	 * 
	 * @param apiType Name of the global type ("id" attribute under "types").
	 */
	public Klass(String apiType) {
		if (apiType == null) {
			throw new IllegalArgumentException("API type must not be null when creating unresolved class references.");
		}
		this.namespace = null;
		this.name = null;
		this.apiType = apiType;
		this.isGlobal = true;
		this.unresolved = true;
	}

	/**
	 * New class by namespace only.
	 * 
	 * This happens only for anonymous item types ("Addon.Details" ->
	 * dependencies) where there is neither a parameter name nor a member name.
	 * 
	 * @param namespace Namespace reference
	 */
	public Klass(Namespace namespace) {
		this(namespace, null, null);
	}

	/**
	 * New class by namespace and variable name.
	 * 
	 * Another anonymous type, but with a given variable name, retrieved from
	 * property name or parameter name. It could also be a computed name for
	 * multitypes.
	 * 
	 * @param namespace Namespace reference
	 * @param name Best guess of name (will be transformed later depending on
	 *            type)
	 */
	public Klass(Namespace namespace, String name) {
		this(namespace, name, null);
	}

	/**
	 * New class for global types.
	 * 
	 * A global type, as defined in introspect's "type" list. The "id" attribute
	 * corresponds to the {@link #apiType} variable.
	 * 
	 * @param namespace Namespace reference
	 * @param name Best guess of name (will be ignored later)
	 * @param apiType Name of global type
	 */
	public Klass(Namespace namespace, String name, String apiType) {
		this.namespace = namespace;
		this.name = name;
		this.apiType = apiType;
		this.unresolved = false;
		if (apiType != null) {
			GLOBALS.put(apiType, this);
		}
	}

	/**
	 * Returns the resolved class object if unresolved or the same instance
	 * otherwise.
	 * 
	 * If this class had only a reference to a global type, it was marked as
	 * unresolved. Later, when all global types are transformed into
	 * {@link Klass} objects (e.g. when rendering), the reference can be
	 * returned via this method.
	 * 
	 * @param klass
	 * @return
	 */
	public static Klass resolve(Klass klass) {
		final Klass resolvedKlass;
		
		// resolve class itself
		if (klass.isUnresolved()) {
			if (!GLOBALS.containsKey(klass.apiType)) {
				throw new RuntimeException("Trying to resolve unknown class \"" + klass.apiType + "\".");
			}
			resolvedKlass = GLOBALS.get(klass.apiType);
		} else {
			resolvedKlass = klass;
		}
		
		// also resolve parent class
		if (resolvedKlass.doesExtend()) {
			resolvedKlass.parentClass = resolve(resolvedKlass.parentClass);
		}
		
		return resolvedKlass;
	}

	/**
	 * Adds type to inner types and updates the reference back
	 * to this instance.
	 * 
	 * @param klass
	 */
	public void linkInnerType(Klass klass) {
		if (unresolved) {
			throw new RuntimeException("Unresolved.");
		}
		innerTypes.add(klass);
		klass.setOuterType(this);
	}
	
	public void linkInnerEnum(Enum e) {
		if (unresolved) {
			throw new RuntimeException("Unresolved.");
		}
		innerEnums.add(e);
		e.setOuterType(this);
	}
	
	/**
	 * Retrieves imports for each module of this class.
	 */
	public void findModuleImports() {
		for (IClassModule module : Introspect.getClassModules()) {
			imports.addAll(module.getImports(this));
		}
	}

	public void addConstructor(Constructor c) {
		if (unresolved) {
			throw new RuntimeException("Unresolved.");
		}
		constructors.add(c);
	}

	public void addMember(Member member) {
		if (unresolved) {
			throw new RuntimeException("Unresolved.");
		}
		members.add(member);
	}

	public void addImport(String i) {
		if (unresolved) {
			throw new RuntimeException("Unresolved.");
		}
		this.imports.add(i);
	}

	public boolean hasInnerTypes() {
		if (unresolved) {
			throw new RuntimeException("Unresolved.");
		}
		return !innerTypes.isEmpty();
	}

	public boolean hasInnerEnums() {
		if (unresolved) {
			throw new RuntimeException("Unresolved.");
		}
		return !innerEnums.isEmpty();
	}

	public boolean isNative() {
		return isNative;
	}

	public void setNative(boolean isNative) {
		if (unresolved) {
			throw new RuntimeException("Unresolved.");
		}
		this.isNative = isNative;
	}

	public boolean isInner() {
		return isInner;
	}

	public void setInner(boolean isInner) {
		if (unresolved) {
			throw new RuntimeException("Unresolved.");
		}
		this.isInner = isInner;
	}

	public boolean isMultiType() {
		if (unresolved) {
			throw new RuntimeException("Unresolved.");
		}
		return isMultiType;
	}

	public void setMultiType(boolean isMultiType) {
		if (unresolved) {
			throw new RuntimeException("Unresolved.");
		}
		this.isMultiType = isMultiType;
	}

	public boolean isArray() {
		return isArray;
	}

	public void setArray(boolean isArray) {
		if (unresolved) {
			throw new RuntimeException("Unresolved.");
		}
		this.isArray = isArray;
	}

	public Klass getArrayType() {
		if (unresolved) {
			throw new RuntimeException("Unresolved.");
		}
		return arrayType;
	}

	public boolean isGlobal() {
		return isGlobal;
	}

	public void setGlobal(boolean isGlobal) {
		if (unresolved) {
			throw new RuntimeException("Unresolved.");
		}
		this.isGlobal = isGlobal;
	}

	public void setArrayType(Klass arrayType) {
		if (unresolved) {
			throw new RuntimeException("Unresolved.");
		}
		this.arrayType = arrayType;
	}

	public String getName() {
		if (unresolved) {
			throw new RuntimeException("Unresolved.");
		}
		return name;
	}

	public Namespace getNamespace() {
		if (unresolved) {
			throw new RuntimeException("Unresolved.");
		}
		return namespace;
	}

	public String getApiType() {
		if (unresolved) {
			throw new RuntimeException("Unresolved.");
		}
		return apiType;
	}

	public List<Constructor> getConstructors() {
		if (unresolved) {
			throw new RuntimeException("Unresolved.");
		}
		return constructors;
	}

	public List<Member> getMembers() {
		if (unresolved) {
			throw new RuntimeException("Unresolved.");
		}
		// sort before return.
		Collections.sort(members, new Comparator<Member>() {
			@Override
			public int compare(Member o1, Member o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		return members;
	}

	public List<Klass> getInnerTypes() {
		if (unresolved) {
			throw new RuntimeException("Unresolved.");
		}
		return innerTypes;
	}

	public List<Enum> getInnerEnums() {
		if (unresolved) {
			throw new RuntimeException("Unresolved.");
		}
		return innerEnums;
	}

	public boolean isUnresolved() {
		return unresolved;
	}

	public Klass getOuterType() {
		return outerType;
	}

	public void setOuterType(Klass outerType) {
		this.outerType = outerType;
	}

	public Klass getParentClass() {
		return parentClass;
	}

	public void setParentClass(Klass parentClass) {
		this.parentClass = parentClass;
	}
	
	public boolean doesExtend() {
		return parentClass != null;
	}

	public Set<String> getImports() {
		final Set<String> imports = new HashSet<String>();

		imports.addAll(this.imports);
		for (Member m : members) {
			if (m.getType() != null) {
				imports.addAll(m.getType().getImports());
			}
		}
		return imports;
	}

}
