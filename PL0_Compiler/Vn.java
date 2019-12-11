package PL0_Compiler;

public enum Vn {
	Program, Procedure,
	ConstDeclaration, NextConst,
	VarDeclaration, NextVar,
	ChildProc,
	Sequence, Statement, NextStatement,
	RelationExp, R,
	Expression, ExpItem,
	Item, NextFact,
	Factor
}
