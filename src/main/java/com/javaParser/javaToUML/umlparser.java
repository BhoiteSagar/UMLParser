package com.javaParser.javaToUML;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.ModifierSet;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;

public class umlparser {
	// URL for the uml diagram generation tool
	private String umlURL = "http://yuml.me/diagram/scruffy/class/";
	// Storing the primitive data types
	private String[] primtiveDataTypes = { "byte", "short", "int", "long", "float", "double", "boolean", "char", "Byte",
			"Short", "Int", "Long", "Float", "Double", "Boolean", "Char" };
	private JSONObject jObj = new JSONObject();

	public umlparser() {
	}

	public static void main(String[] args) {
		//String javaFileLocation = args[0];
		String javaFileLocation = "/Users/Sagar/Downloads/javasrc-class-diagram-2/javasrc";
		//String imageName = args[1];
		String imageName = "image.jpg";
		umlparser umlp = new umlparser();
		umlp.parseInputFiles(javaFileLocation, imageName);
	}

	public void parseInputFiles(String javaFileLocation, String imageName) {
		try {
			// location of the java files
			File fileLocation = new File(javaFileLocation);

			// fetch java files from the location
			for (File javaFile : fileLocation.listFiles(fetchJavaFiles)) {
				// input stream for the files
				FileInputStream fis = new FileInputStream(javaFile.getAbsolutePath());

				String className = "", classAttributes = "", classMethods = "";
				String interfaceName = "", objAttributes = "";
				JSONArray classUses = new JSONArray();
				ArrayList<String> classImplements = new ArrayList<String>();
				ArrayList<String> classExtends = new ArrayList<String>();
				JSONArray associationRelationsArray = new JSONArray();
				boolean isObj = false;
				String objStr = "", objNameStr = "";

				// parsing the input stream using javaparser
				CompilationUnit cu = JavaParser.parse(fis);

				// fetch the child nodes
				List<Node> childrenNodes = cu.getChildrenNodes();

				// traverse the child nodes for getting the parsed classes and interfaces
				for (Node childNode : childrenNodes) {
					if (childNode instanceof ClassOrInterfaceDeclaration) {
						ClassOrInterfaceDeclaration coid = (ClassOrInterfaceDeclaration) childNode;

						// Differentiate between class and interface
						if (coid.toString().contains("interface")) {
							interfaceName = coid.getName();
						} else {
							className = coid.getName();

							if (coid.getExtends() != null) {
								classExtends
										.add(coid.getExtends().toString().replaceAll("\\[", "").replaceAll("\\]", ""));
							}
							// get the interface relation
							if (coid.getImplements() != null) {
								List<ClassOrInterfaceType> interfaces = coid.getImplements();
								// traverse multiple interface relations
								for (ClassOrInterfaceType temp : interfaces) {
									classImplements.add(temp.toString());
								}
							}
						}
					} else if (childNode instanceof PackageDeclaration) {
						// PackageDeclaration pdn = (PackageDeclaration)childNode;
					} else if (childNode instanceof ImportDeclaration) {
						// ImportDeclaration idn = (ImportDeclaration)childNode;
					}
				}
				// fetch the type declarations in the body
				List<TypeDeclaration> listOfTypeDeclarations = cu.getTypes();

				for (TypeDeclaration td : listOfTypeDeclarations) {

					List<BodyDeclaration> bodyDeclarations = td.getMembers();

					for (int i = 0; i < bodyDeclarations.size(); i++) {

						BodyDeclaration bdn = bodyDeclarations.get(i);

						if (bdn instanceof FieldDeclaration) {
							FieldDeclaration fdn = (FieldDeclaration) bdn;
							String variableDataType = "", variableModifier = "", variableName = "";
							boolean checkPrimitive = false;
							String genericsObject = "";

							int accessModifiers = fdn.getModifiers();
							variableModifier = getModifier(accessModifiers, "");

							List<Node> nodesList = fdn.getChildrenNodes();

							for (Node nl : nodesList) {
								if (nl instanceof ReferenceType) {
									ReferenceType rft = (ReferenceType) nl;
									Type refType = rft.getType();

									for (String str : primtiveDataTypes) {
										if (refType.toString().contains(str)) {
											variableDataType += refType.toString() + "(*)";
											checkPrimitive = true;
											break;
										}
									}

									JSONArray associationArray = new JSONArray();
									if (!checkPrimitive) {
										String genericsCheck = rft.getType().toString();

										if (genericsCheck.contains("<") && genericsCheck.contains(">")) {
											genericsObject = genericsCheck.substring(genericsCheck.indexOf("<") + 1,
													genericsCheck.indexOf(">"));

											objStr = genericsObject;
											isObj = true;

											associationArray.put(className);
											associationArray.put("-*");
											associationArray.put(genericsObject);
											associationRelationsArray.put(associationArray);
										} else if (genericsCheck.equals("String")) {
											checkPrimitive = true;
											variableDataType += genericsCheck;
										} else {
											objStr = rft.getType().toString();
											isObj = true;

											associationArray.put(className);
											associationArray.put("1-1");
											associationArray.put(rft.getType().toString());
											associationRelationsArray.put(associationArray);
										}
									}
								} else if (nl instanceof PrimitiveType) {
									PrimitiveType prt = (PrimitiveType) nl;
									checkPrimitive = true;
									variableDataType += prt.toString();
								} else if (nl instanceof VariableDeclarator) {
									VariableDeclarator vdr = (VariableDeclarator) nl;
									variableName = vdr.toString();
									if(isObj) {
										objNameStr = variableName;
										objAttributes += variableModifier + objNameStr + ":" + objStr + ";";
										isObj = false;
									}
								}
							}

							if (checkPrimitive && variableModifier!="#" && variableModifier!="") {
								classAttributes += variableModifier + variableName + ":" + variableDataType + ";";
							}
						} else if (bdn instanceof MethodDeclaration) {
							MethodDeclaration mdn = (MethodDeclaration) bdn;
							int accessModifiers = mdn.getModifiers();
							String methodModifier = "";
							Boolean objFlag = false;

							if (mdn.getName().equals("main")) {
								List<Statement> statementList = mdn.getBody().getStmts();
								for (Statement pmt : statementList) {
									String mystring = pmt.toString();
									String[] arr = mystring.split(" ", 2);

									classUses.put("＜＜interface＞＞;" + arr[0]);
								}
							}

							accessModifiers = Integer.parseInt(Integer.toString(accessModifiers).substring(0, 1));
							methodModifier = getModifier(accessModifiers, "methodModifier");

							List<Parameter> paramtersList = mdn.getParameters();

							String parameterModifier = "", parameterName = "", parameterString = "";
							for (Parameter pmt : paramtersList) {
								List<Node> childNodeList = pmt.getChildrenNodes();

								for (Node nd : childNodeList) {
									if (nd instanceof ReferenceType) {
										/*ReferenceType reft = (ReferenceType) nd;
										Type reType = reft.getType();*/
										parameterModifier += nd.toString() + ";";
										if(interfaceName.equals("") && !nd.toString().contains("String")) {
											objFlag = true;
											if(!classUses.toString().contains("＜＜interface＞＞;" + nd.toString()))
												classUses.put("＜＜interface＞＞;" + nd.toString());
										}
									} else if (nd instanceof PrimitiveType) {
										parameterModifier += nd.toString() + ";";
									} else {
										parameterName = nd.toString();
									}

								}
								parameterString += parameterName + ":" + parameterModifier.replaceAll("\\[", "［").replaceAll("\\]", "］");
							}

							if (parameterString != null && parameterString.length() > 1) {
								parameterString = parameterString.substring(0, parameterString.length() - 1);
							}

							if(methodModifier!="#" && methodModifier!="-" && !objFlag) {
								classMethods += methodModifier + mdn.getName() + "(" + parameterString + "):"
										+ mdn.getType() + ";";
							}
						} else if (bdn instanceof ConstructorDeclaration) {
							ConstructorDeclaration constructor = (ConstructorDeclaration) bdn;

							List<Parameter> paramtersList = constructor.getParameters();
							String consParam = "", consName = "", consType = "";
							for (Parameter pmt : paramtersList) {
								List<Node> childNodeList = pmt.getChildrenNodes();

								for (Node nd : childNodeList) {
									if (nd instanceof ReferenceType) {
										consType = nd.toString();
									} else {
										consName = nd.toString();
									}
								}
								consParam += consName + ":" + consType;
								classUses.put("＜＜interface＞＞;" + consType);
							}
							classMethods += "+" + constructor.getName() + "(" + consParam + ");";
						}
					}

					JSONObject obj = new JSONObject();
					obj.put("classAttributes", classAttributes);
					obj.put("classMethods", classMethods);
					obj.put("classAssociations", associationRelationsArray);
					obj.put("implements", classImplements);
					obj.put("extends", classExtends);
					obj.put("uses", classUses);
					obj.put("objAttributes", objAttributes);
					if (interfaceName == "") {
						jObj.put(className, obj);
					} else {
						interfaceName = "＜＜interface＞＞;" + interfaceName;
						jObj.put(interfaceName, obj);
					}
				}
			}

			String umlInput = generateUMLInput(jObj);
			System.out.println(umlInput);
			umlInput = URLEncoder.encode(umlInput.replaceAll("\"", "＂"), "UTF-8");
			umlURL += umlInput;

			saveImage(umlURL, imageName);
			
			showImage(imageName);

		} catch (Exception e) { 
			System.out.println("Error: " + e.getMessage() + " " + e.getLocalizedMessage());
			e.printStackTrace();
			System.out.println(e.initCause(e) );
		}
	}
	
	public static void showImage(String imageName) {
		
		try {
			BufferedImage img = ImageIO.read(new File(imageName));
			ImageIcon icon = new ImageIcon(img);
			JLabel label = new JLabel(icon);
			JScrollPane scroller = new JScrollPane(label, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			JFrame frame = new JFrame("UML Class Diagram");
			frame.getContentPane().add(scroller);
		    frame.setSize(1000, 3000);
		    frame.setVisible(true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String getModifier(int modifier, String type) {
		String modifierType = "";
		switch (modifier) {
			case ModifierSet.PRIVATE:
				modifierType = "-";
				break;
			case ModifierSet.PUBLIC:
				modifierType = "+";
				break;
			case ModifierSet.PROTECTED:
				modifierType = "#";
				break;
			default:
				if(type == "methodModifier") {
					modifierType = "+";
				}
				break;
		}
		return modifierType;
	}

	public static String generateUMLInput(JSONObject jsonObject) throws ParseException, JSONException {

		Iterator<?> iterator = jsonObject.keys();
		String parsedString = "", classString = "";
		ArrayList<String> classNameArray = new ArrayList<String>();
		ArrayList<String> relations = new ArrayList<String>();
		HashMap<String, String> classStructureArray = new HashMap<String, String>();
		classNameArray.clear();

		while (iterator.hasNext()) {

			String obj = iterator.next().toString();

			if (jsonObject.get(obj) instanceof JSONObject) {
				generateUMLInput((JSONObject) jsonObject.get(obj));
				classNameArray.add(obj.toString());
			}
		}

		for (String temp : classNameArray) {

			String methodString = jsonObject.getJSONObject(temp).getString("classMethods").toString();
			String attributeString = jsonObject.getJSONObject(temp).getString("classAttributes").toString();
			String objAttributeString = jsonObject.getJSONObject(temp).getString("objAttributes").toString();

			String methodName = "", attributeName = "", newMethodString = "";
			boolean setBoolean = false, getBoolean = false;

			for (String retval : methodString.split(";")) {

				if (retval.contains("get")) {
					methodName = retval.substring(1, retval.indexOf("("));
					attributeName = methodName.substring(3, methodName.length()).toLowerCase();
					getBoolean = true;
				}
				if (retval.toLowerCase().contains(("set" + attributeName))) {
					setBoolean = true;
				}

				if (setBoolean && getBoolean) {
					if (attributeString.contains(attributeName)) {
						StringBuilder aStr = new StringBuilder(attributeString);
						int index = attributeString.indexOf(attributeName) - 1;
						aStr.setCharAt(index, '+');
						attributeString = aStr.toString();

						for (String mval : methodString.split(";")) {

							if (mval.toLowerCase().contains(methodName.toLowerCase()) || mval.toLowerCase().contains(("set" + attributeName))) {

							} else {
								newMethodString += mval+";";
							}
						}
						methodString = newMethodString;
						
						setBoolean = false; getBoolean = false;
						methodName = ""; attributeName = "";
					}
				}
			}
			
			for (String val : objAttributeString.split(";")) {

				if(!classNameArray.contains(val.substring(val.indexOf(":")+1, val.length())) && !classNameArray.contains("＜＜interface＞＞;"+(val.substring(val.indexOf(":")+1, val.length())))){
					attributeString += val+";";
				}
			}
			
			if (attributeString != null && attributeString.length() > 1) {
				attributeString = attributeString.substring(0, attributeString.length() - 1);
			}

			if (temp.contains("＜＜interface＞＞")) {
				classString = "[" + temp;
				if (methodString != null && methodString.length() > 1) {
					classString += "||" + methodString + "]";
				} else {
					classString += "]";
				}
			} else {
				classString = "[" + temp;
				if (attributeString != null && attributeString.length() > 1) {
					classString += "|" + attributeString;
				} else {
					classString += "|";
				}
				if (methodString != null && methodString.length() > 1) {
					classString += "|" + methodString + "]";
				} else {
					classString += "]";
				}
			}

			parsedString += classString;
			classStructureArray.put(temp, classString);
		}

		String classKey = "";
		ArrayList<String> tempAssoArr = new ArrayList<String>();
		for (String temp : classNameArray) {

			if (!jsonObject.getJSONObject(temp).get("implements").toString().equals("[]")) {

				String z = jsonObject.getJSONObject(temp).get("implements").toString().replaceAll("\\[", "")
						.replaceAll("\\]", "");
				StringTokenizer st = new StringTokenizer(z, ", ");

				while (st.hasMoreTokens()) {
					relations.add(classStructureArray.get("＜＜interface＞＞;" + st.nextToken()) + "^-.-"
							+ classStructureArray.get(temp));
				}
			}
			if (!jsonObject.getJSONObject(temp).get("extends").toString().equals("[]")) {

				classKey = jsonObject.getJSONObject(temp).get("extends").toString().replaceAll("\\[", "")
						.replaceAll("\\]", "");
				relations.add(classStructureArray.get(classKey) + "^-" + classStructureArray.get(temp));
			}
			if (!jsonObject.getJSONObject(temp).get("uses").toString().equals("[]")) {

				String z = jsonObject.getJSONObject(temp).get("uses").toString().replaceAll("\\[", "")
						.replaceAll("\\]", "").replace("\"", "");

				for (String retval : z.split(",")) {

					if (classStructureArray.get(retval) != null)
						relations.add(classStructureArray.get(temp) + "uses-.->" + classStructureArray.get(retval));
				}
			}
			if (!jsonObject.getJSONObject(temp).get("classAssociations").toString().equals("[]")) {

				JSONObject getObject = jsonObject.getJSONObject(temp);
				JSONArray getArray = getObject.getJSONArray("classAssociations");
				ArrayList<String> list = new ArrayList<String>();

				for (int i = 0; i < getArray.length(); i++) {
					JSONArray objects = getArray.getJSONArray(i);
					list.add(objects.toString());
				}

				ArrayList<String> assoArr = new ArrayList<String>();

				for (String tempp : list) {
					String z = tempp.replaceAll("\\[", "").replaceAll("\\]", "").replace("\"", "");

					StringTokenizer st = new StringTokenizer(z, ",");
					assoArr.clear();
					while (st.hasMoreTokens()) {
						assoArr.add(st.nextToken());
					}

					ArrayList<String> sortedAssoArr = new ArrayList<String>(assoArr);
					sortedAssoArr.remove(1);
					Collections.sort(sortedAssoArr);
					String arrElement = sortedAssoArr.toString().replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\, ", "");

					if(!tempAssoArr.contains(arrElement)) {
						tempAssoArr.add(arrElement);
						if(classNameArray.contains("＜＜interface＞＞;"+assoArr.get(2))){
							relations.add(classStructureArray.get(assoArr.get(0)) + assoArr.get(1)+ classStructureArray.get("＜＜interface＞＞;"+assoArr.get(2)));
						} else {
							relations.add(classStructureArray.get(assoArr.get(0)) + assoArr.get(1)+ classStructureArray.get(assoArr.get(2)));
						}
					}
				}
			}
		}
		parsedString = relations.toString();

		String allClassString = "";
		Iterator<?> it = classStructureArray.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        allClassString += ","+pair.getValue();
	        it.remove(); // avoids a ConcurrentModificationException
	    }

		return parsedString+allClassString;
	}

	// function to get the image from uml diagram generator
	public static void saveImage(String imageUrl, String destinationFile) throws IOException {
		URL url = new URL(imageUrl);
		InputStream is = url.openStream();
		OutputStream os = new FileOutputStream(destinationFile);

		byte[] b = new byte[2048];
		int length;

		while ((length = is.read(b)) != -1) {
			os.write(b, 0, length);
		}

		is.close();
		os.close();
	}

	// Using file filter to fetch java files
	FileFilter fetchJavaFiles = new FileFilter() {
		public boolean accept(File file) {
			if (file.isDirectory()) {
				return true;
			}
			return file.getName().endsWith(".java");
		}
	};
}
