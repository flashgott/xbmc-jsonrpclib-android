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
package org.xbmc.android.jsonrpc.generator.view;

import org.xbmc.android.jsonrpc.generator.model.JavaClass;
import org.xbmc.android.jsonrpc.generator.model.JavaConstructor;
import org.xbmc.android.jsonrpc.generator.model.JavaEnum;
import org.xbmc.android.jsonrpc.generator.view.module.IClassModule;

/**
 * Renders a Java class.
 * 
 * @author freezy <freezy@xbmc.org>
 */
public class ClassView extends AbstractView {

	private final JavaClass klass;

	public ClassView(JavaClass klass) {
		this.klass = klass;
	}

	public void renderDeclaration(StringBuilder sb, int idt, boolean force) {

		final String indent = getIndent(idt);

		// signature
		sb.append("\n");
		sb.append(indent).append("public static class ");
		sb.append(getClassName(klass));
		if (klass.hasParentModule()) {
			klass.getParentModule().renderExtends(sb, klass);
		}
		sb.append(" {\n");
		
		// api type
		if (klass.getApiType() != null) {
			sb.append(indent).append("	public final static String API_TYPE = \"");
			sb.append(klass.getApiType());
			sb.append("\";\n");
		}
		
		// render modules
		for (IClassModule module : klass.getClassModules()) {
			module.render(sb, klass.getNamespace(), klass, idt + 1);
		}
		
		// constructors
		if (!klass.doesExtend()) {
			for (JavaConstructor c : klass.getConstructors()) {
				final ConstructorView constructorView = new ConstructorView(c);
				constructorView.renderDeclaration(sb, klass.getNamespace(), idt + 1);
			}
		}

		// inner classes
		if (klass.hasInnerTypes()) {
			for (JavaClass innerClass : klass.getInnerTypes()) {
				final ClassView classView = new ClassView(innerClass);
				classView.renderDeclaration(sb, idt + 1, true);
			}
		}

		// inner enums
		if (klass.hasInnerEnums()) {
			for (JavaEnum e : klass.getInnerEnums()) {
				final EnumView enumView = new EnumView(e);
				enumView.render(sb, idt + 1, true);
			}
		}
		
		sb.append(indent).append("}\n");
	}


}
