/*
 * @(#)Compiler.java                       
 * 
 * Revisions and updates (c) 2022-2024 Sandy Brownlee. alexander.brownlee@stir.ac.uk
 * 
 * Original release:
 *
 * Copyright (C) 1999, 2003 D.A. Watt and D.F. Brown
 * Dept. of Computing Science, University of Glasgow, Glasgow G12 8QQ Scotland
 * and School of Computer and Math Sciences, The Robert Gordon University,
 * St. Andrew Street, Aberdeen AB25 1HG, Scotland.
 * All rights reserved.
 *
 * This software is provided free for educational use only. It may
 * not be used for commercial purposes without the prior written permission
 * of the authors.
 */

package triangle;

import com.sampullara.cli.Argument;
import triangle.abstractSyntaxTrees.Program;
import triangle.codeGenerator.Emitter;
import triangle.codeGenerator.Encoder;
import triangle.contextualAnalyzer.Checker;
import triangle.optimiser.ConstantFolder;
import triangle.syntacticAnalyzer.Parser;
import triangle.syntacticAnalyzer.Scanner;
import triangle.syntacticAnalyzer.SourceFile;
import triangle.treeDrawer.Drawer;

import com.sampullara.cli.Args;

import java.util.List;

/**
 * The main driver class for the Triangle compiler.
 *
 * @version 2.1 7 Oct 2003
 * @author Deryck F. Brown
 * Also SMH
 */
public class Compiler {

	/** The filename for the object program, normally obj.tam. */
	@Argument(alias = "objectName", description = "name of object", required = false)
	static String objectName = "obj.tam";
	@Argument(alias = "showTree", description = "display tree flag", required = false)
	static boolean showTree = false;
	@Argument(alias = "folding", description = "loop optimisation flag", required = false)
	static boolean folding = false;
	@Argument(alias = "showTreeAfter", description = "display tree flag after folding", required = false)
	static boolean showTreeAfter = false;

	private static Scanner scanner;
	private static Parser parser;
	private static Checker checker;
	private static Encoder encoder;
	private static Emitter emitter;
	private static ErrorReporter reporter;
	private static Drawer drawer;

	/** The AST representing the source program. */
	private static Program theAST;

	/**
	 * Compile the source program to TAM machine code.
	 *
	 * @param sourceName   the name of the file containing the source program.
	 * @param objectName   the name of the file containing the object program.
	 * @param showingAST   true iff the AST is to be displayed after contextual
	 *                     analysis
	 * @param showingTable true iff the object description details are to be
	 *                     displayed during code generation (not currently
	 *                     implemented).
	 * @return true iff the source program is free of compile-time errors, otherwise
	 *         false.
	 */
	static boolean compileProgram(String sourceName, String objectName, boolean showingAST, boolean showingTable) {

		System.out.println("********** " + "Triangle Compiler (Java Version 2.1)" + " **********");

		System.out.println("Syntactic Analysis ...");
		SourceFile source = SourceFile.ofPath(sourceName);

		if (source == null) {
			System.out.println("Can't access source file " + sourceName);
			System.exit(1);
		}

		scanner = new Scanner(source);
		reporter = new ErrorReporter(false);
		parser = new Parser(scanner, reporter);
		checker = new Checker(reporter);
		emitter = new Emitter(reporter);
		encoder = new Encoder(emitter, reporter);
		drawer = new Drawer();

		// scanner.enableDebugging();
		theAST = parser.parseProgram(); // 1st pass
		if (reporter.getNumErrors() == 0) {
			// if (showingAST) {
			// drawer.draw(theAST);
			// }
			System.out.println("Contextual Analysis ...");
			checker.check(theAST); // 2nd pass
			if (showingAST) {
				drawer.draw(theAST);
			}
			if (folding) {
				theAST.visit(new ConstantFolder());
				if(showTreeAfter){
					drawer.draw(theAST);
				}
			}
			
			if (reporter.getNumErrors() == 0) {
				System.out.println("Code Generation ...");
				encoder.encodeRun(theAST, showingTable); // 3rd pass
			}
		}

		boolean successful = (reporter.getNumErrors() == 0);
		if (successful) {
			emitter.saveObjectProgram(objectName);
			System.out.println("Compilation was successful.");
		} else {
			System.out.println("Compilation was unsuccessful.");
		}
		return successful;
	}

	/**
	 * Triangle compiler main program.
	 *
	 * @param args the only command-line argument to the program specifies the
	 *             source filename.
	 */
	public static void main(String[] args) {

		List<String> unparsed = Args.parseOrExit(Compiler.class, args);
		if (unparsed.size() != 1) { // The only unparsed arg should be the filename
			// Args.usage(Compiler.class); Cannot use this, does not display the filename requirement
			System.out.println("******************************");
			System.out.println("Usage:");
			System.out.println("  FileName");
			System.out.println("  -objectName (-objectName) [String] name of object (obj.tam)");
			System.out.println("  -showTree (-showTree) [flag] display tree flag");
			System.out.println("  -folding (-folding) [flag] loop optimisation flag");
			System.out.println("  -showTreeAfter (-showTreeAfter) [flag] display tree flag after folding");
			System.out.println("******************************");
			System.exit(1);
		}

		String sourceName = unparsed.get(0);
		
		var compiledOK = compileProgram(sourceName, objectName, showTree, false);

		if (!showTree) {
			System.exit(compiledOK ? 0 : 1);
		}
	}
	
	//private static void parseArgs(String[] args) {
	//	for (String s : args) {
	//		var sl = s.toLowerCase();
	//		if (sl.equals("tree")) {
	//			showTree = true;
	//		} else if (sl.startsWith("-o=")) {
	//			objectName = s.substring(3);
	//		} else if (sl.equals("folding")) {
	//			folding = true;
	//		}
	//	}
	//}
}
