package PL0_Compiler;

/**
 *　各种符号的编码
 */
public enum Symbol {
	nul, ident, number, plus, minus, times, slash,
	oddsym, eql, neq, lss, leq, gtr, geq, lparen, rparen,
	comma, semicolon, period, becomes, 
	beginsym, endsym, ifsym, thensym, whilesym,
	writesym, readsym, dosym, callsym, constsym, varsym, procsym,
	integersym, booleansym, truesym, falsesym, forsym, tosym, downtosym, and, or, not, colon
}