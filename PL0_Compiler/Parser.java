package PL0_Compiler;

import java.util.Stack;

/**
 *　　语法分析器。这是PL/0分析器中最重要的部分，在语法分析的过程中穿插着语法错误检查和目标代码生成。
 */
public class Parser {
	private Scanner lex;					// 对词法分析器的引用
	private Table table;					// 对符号表的引用
	private Interpreter interp;				// 对目标代码生成器的引用
	private Grammar ga;

	private final int symnum = Symbol.values().length;
	
	// 表示声明开始的符号集合、表示语句开始的符号集合、表示因子开始的符号集合
	// 实际上这就是声明、语句和因子的FIRST集合
	private SymSet declbegsys, statbegsys, facbegsys;
	
	/**
	 * 当前符号，由nextsym()读入
	 * @see #nextSym()
	 */
	private Symbol sym;
	
	/**
	 * 当前作用域的堆栈帧大小，或者说数据大小（data size）
	 */
	private int dx = 0;
	
	/**
	 * 构造并初始化语法分析器，这里包含了C语言版本中init()函数的一部分代码
	 * @param l 编译器的词法分析器
	 * @param t 编译器的符号表
	 * @param i 编译器的目标代码生成器
	 */
	public Parser(Scanner l, Table t, Interpreter i) {
		lex = l;
		table = t;
		interp = i;
		ga = new Grammar();

		// 设置声明开始符号集
		declbegsys = new SymSet(symnum);
		declbegsys.set(Symbol.constsym);
		declbegsys.set(Symbol.varsym);
		declbegsys.set(Symbol.procsym);

		// 设置语句开始符号集
		statbegsys = new SymSet(symnum);
		statbegsys.set(Symbol.beginsym);
		statbegsys.set(Symbol.callsym);
		statbegsys.set(Symbol.ifsym);
		statbegsys.set(Symbol.whilesym);
		statbegsys.set(Symbol.forsym);
		statbegsys.set(Symbol.readsym);			// thanks to elu
		statbegsys.set(Symbol.writesym);

		// 设置因子开始符号集
		facbegsys = new SymSet(symnum);
		facbegsys.set(Symbol.ident);
		facbegsys.set(Symbol.number);
		facbegsys.set(Symbol.lparen);

	}
	
	/**
	 * 启动语法分析过程，此前必须先调用一次nextsym()
	 * @see #nextSym()
	 */
	public void parse() {
		SymSet nxtlev = new SymSet(symnum);
		nxtlev.or(declbegsys);
		nxtlev.or(statbegsys);
		nxtlev.set(Symbol.period);
		parseBlock(0, nxtlev);
		
		if (sym != Symbol.period)
			Err.report(9);
	}
	
	/**
	 * 获得下一个语法符号，这里只是简单调用一下getsym()
	 */
	public void nextSym() {
		lex.getsym();
		sym =lex.sym;
	}
	
	/**
	 * 测试当前符号是否合法
	 * 
	 * @param s1 我们需要的符号
	 * @param s2 如果不是我们需要的，则需要一个补救用的集合
	 * @param errcode 错误号
	 */
	void test(SymSet s1, SymSet s2, int errcode) {
		// 在某一部分（如一条语句，一个表达式）将要结束时时我们希望下一个符号属于某集合
		//（该部分的后跟符号），test负责这项检测，并且负责当检测不通过时的补救措施，程
		// 序在需要检测时指定当前需要的符号集合和补救用的集合（如之前未完成部分的后跟符
		// 号），以及检测不通过时的错误号。
		if (!s1.get(sym)) {
			Err.report(errcode);
			// 当检测不通过时，不停获取符号，直到它属于需要的集合或补救的集合
			while (!s1.get(sym) && !s2.get(sym))
				nextSym();
		}
	}
	
	/**
	 * 分析<分程序>
	 * 
	 * @param lev 当前分程序所在层
	 * @param fsys 当前模块后跟符号集
	 */

	public void parseBlock(int lev, SymSet fsys)
	{
		// <分程序> := [<常量说明部分>][<变量说明部分>][<过程说明部分>]<语句>

		int dx0, tx0, cx0;				// 保留初始dx，tx和cx
		SymSet nxtlev = new SymSet(symnum);

		dx0 = dx;						// 记录本层之前的数据量（以便恢复）
		dx = 3;
		tx0 = table.tx;					// 记录本层名字的初始位置（以便恢复）
		table.get(table.tx).adr = interp.cx;

		interp.gen(Fct.JMP, 0, 0);

		if (lev > PL0.levmax)
			Err.report(32);

		Stack<Object> ss = new Stack<>();		// 初始化分析栈
		ss.push(Symbol.period);
		ss.push(Vn.Procedure);
		Object prod[], pk;
		while(sym != Symbol.period && !ss.isEmpty())
		{
			pk = ss.pop();
			if(pk instanceof Vn)
			{
				if(pk == Vn.ConstDeclaration)
					parseConstDeclaration(lev);
				else if(pk == Vn.VarDeclaration)
					parseVarDeclaration(lev);
				else if(pk == Vn.ChildProc)
				{
					nxtlev = (SymSet) fsys.clone();
					nxtlev.set(Symbol.semicolon);
					parseBlock(lev+1, nxtlev);
					nxtlev = (SymSet) statbegsys.clone();
					nxtlev.set(Symbol.ident);
					nxtlev.set(Symbol.procsym);
					if(ss.peek() == Symbol.semicolon)
					{
						if(sym != Symbol.semicolon)
							Err.report(5);			// 缺少分号
						else {
							ss.pop();
							nextSym();
						}
					}
					test(nxtlev, fsys, 6);
				}
				else if(pk == Vn.Sequence)					// 语句序列
					break;
				else
				{
					prod = ga.select((Vn)pk, sym);
					if(prod != null) {
						for(int i=prod.length-1;i>=0;i--)
							ss.push(prod[i]);
					}
					else
						Err.report(7);				// 无合适文法
				}
			}
			else
			{
				while(pk == Symbol.nul)
					pk = ss.pop();
				if(pk != sym)								// 非终结符不匹配
					Err.report(5);
				else {
					nextSym();
					if(pk == Symbol.procsym)				// 此时sym指向ident
						table.enter(Objekt.procedure, lev+1, dx);
					else if(pk == Symbol.semicolon)
					{
					    //分析完该部分的一个语法单位后检测
						nxtlev = (SymSet) statbegsys.clone();
						nxtlev.or(declbegsys);
						nxtlev.set(Symbol.ident);
						test(nxtlev, declbegsys, 7);
					}
				}
			}
		}

		// 开始生成当前过程代码
		Table.Item item = table.get(tx0);
		interp.code[item.adr].a = interp.cx;
		item.adr = interp.cx;					// 当前过程代码地址
		item.size = dx;							// 声明部分中每增加一条声明都会给dx增加1，
		// 声明部分已经结束，dx就是当前过程的堆栈帧大小
		cx0 = interp.cx;
		interp.gen(Fct.INT, 0, dx);			// 生成分配内存代码

		table.debugTable(tx0);

		// 分析<语句>
		nxtlev = (SymSet) fsys.clone();		// 每个后跟符号集和都包含上层后跟符号集和，以便补救
		nxtlev.set(Symbol.semicolon);		// 语句后跟符号为分号或end
		nxtlev.set(Symbol.endsym);
		parseStatement(nxtlev, lev);
		interp.gen(Fct.OPR, 0, 0);		// 每个过程出口都要使用的释放数据段指令

		nxtlev = new SymSet(symnum);		// 分程序没有补救集合
		test(fsys, nxtlev, 8);		// 检测后跟符号正确性

		interp.listcode(cx0);

		dx = dx0;							// 恢复堆栈帧计数器
		table.tx = tx0;						// 回复名字表位置
	}

	/**
	 * 分析<常量说明部分>
	 * @param lev 当前所在的层次
	 */
	void parseConstDeclaration(int lev) {
		if (sym == Symbol.ident) {
			nextSym();
			if (sym == Symbol.eql || sym == Symbol.becomes) {
				if (sym == Symbol.becomes) 
					Err.report(1);			// 把 = 写成了 :=
				nextSym();
				if (sym == Symbol.number) {
					table.enter(Objekt.constant, lev, dx);
					nextSym();
				} else {
					Err.report(2);			// 常量说明 = 后应是数字
				}
			} else {
				Err.report(3);				// 常量说明标识后应是 =
			}
		} else {
			Err.report(4);					// const 后应是标识符
		}
	}

	/**
	 * 分析<变量说明部分>
	 * @param lev 当前层次
	 */
	void parseVarDeclaration(int lev) {
	    if(sym == Symbol.ident) {
	    	parseVarDecl(lev);
			while(sym == Symbol.ident)
				parseVarDecl(lev);
		} else
			Err.report(4);					// var后应为标识符
	}

	void parseVarDecl(int lev) {
		int tx0 = table.tx;							// 注意tx总是指向最后一个符号项
		table.enter(Objekt.integer, lev, dx++);
		nextSym();
		while(sym == Symbol.comma) {
			nextSym();
			if(sym == Symbol.ident) {
				table.enter(Objekt.integer, lev, dx++);
				nextSym();
			} else
				Err.report(4);
		}
		if(sym == Symbol.colon) {
			nextSym();
			if(sym == Symbol.booleansym) {
			    nextSym();
				while (++tx0 <= table.tx)
					table.get(tx0).kind = Objekt.bool;
			}
			else if(sym == Symbol.integersym)
				nextSym();
			else
				Err.report(-6);				// 无法识别的类型
		} else
			Err.report(-5);					// 缺少冒号
	}

	/**
	 * 分析<语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	void parseStatement(SymSet fsys, int lev) {
		SymSet nxtlev;
		// Wirth 的 PL/0 编译器使用一系列的if...else...来处理
		// 但是你的助教认为下面的写法能够更加清楚地看出这个函数的处理逻辑
		switch (sym) {
		case ident:
			parseAssignStatement(fsys, lev);
			break;
		case readsym:
			parseReadStatement(fsys, lev);
			break;
		case writesym:
			parseWriteStatement(fsys, lev);
			break;
		case callsym:
			parseCallStatement(fsys, lev);
			break;
		case ifsym:
			parseIfStatement(fsys, lev);
			break;
		case beginsym:
			parseBeginStatement(fsys, lev);
			break;
		case whilesym:
			parseWhileStatement(fsys, lev);
			break;
		case forsym:
			parseForStatement(fsys, lev);
			break;
		default:
			nxtlev = new SymSet(symnum);
			test(fsys, nxtlev, 19);
			break;
		}
	}

	/**
	 * 分析<for型循环语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseForStatement(SymSet fsys, int lev) {
		int cx1;

		nextSym();
		if(sym == Symbol.ident) {
			int i = table.position(lex.id);
			if (i > 0) {
				Table.Item item = table.get(i);
				if(item.kind == Objekt.integer) {
				    nextSym();
				    if(sym == Symbol.becomes) {
				    	nextSym();
				    	if(sym == Symbol.number) {
				    		int num = lex.num;
							interp.gen(Fct.LIT, 0, lex.num);
							interp.gen(Fct.STO, lev - item.level, item.adr);			// 给变量赋值
							nextSym();
							if(sym == Symbol.tosym || sym == Symbol.downtosym) {
								boolean is_to = sym == Symbol.tosym;
								nextSym();
								if(sym == Symbol.number) {
									if((is_to && num<=lex.num) || (!is_to && num>=lex.num)) {		// 第一次循环必定执行，否则不生成代码
										nextSym();
										cx1 = interp.cx;											// 语句起始位置
										parseStatement(fsys, lev);
										interp.gen(Fct.LIT, 0, lex.num);
										interp.gen(Fct.LIT, 0, is_to ? 1 : -1);
										interp.gen(Fct.LOD, lev - item.level, item.adr);			// 变量载入栈顶
										interp.gen(Fct.OPR, 0, 2);
										interp.gen(Fct.STO, lev - item.level, item.adr);			// 修改变量值
										interp.gen(Fct.LOD, lev - item.level, item.adr);
										interp.gen(Fct.OPR, 0, is_to ? 10 : 12);					// 循环条件判断
										interp.gen(Fct.JPC, 0, cx1);
									}
								} else
									Err.report(-3);
							} else
								Err.report(-4);	// 此处应为to
						} else
							Err.report(-3);		// 此处应为数值
					} else
						Err.report(-2);			// 此处应为赋值号
				} else
					Err.report(-1);		// 标识符应为变量类型
			} else
				Err.report(11);			// 标识符未声明
		} else
			Err.report(4);				// for后应跟标识符
	}

	/**
	 * 分析<当型循环语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseWhileStatement(SymSet fsys, int lev) {
		int cx1, cx2;
		SymSet nxtlev;
		
		cx1 = interp.cx;						// 保存判断条件操作的位置
		nextSym();
		nxtlev = (SymSet) fsys.clone();
		nxtlev.set(Symbol.dosym);				// 后跟符号为do
		parseCondition(nxtlev, lev);			// 分析<条件>
		cx2 = interp.cx;						// 保存循环体的结束的下一个位置
		interp.gen(Fct.JPC, 0, 0);				// 生成条件跳转，但跳出循环的地址未知
		if (sym == Symbol.dosym)
			nextSym();
		else
			Err.report(18);						// 缺少do
		parseStatement(fsys, lev);				// 分析<语句>
		interp.gen(Fct.JMP, 0, cx1);			// 回头重新判断条件
		interp.code[cx2].a = interp.cx;			// 反填跳出循环的地址，与<条件语句>类似
	}

	/**
	 * 分析<复合语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseBeginStatement(SymSet fsys, int lev) {
		SymSet nxtlev;
		
		nextSym();
		nxtlev = (SymSet) fsys.clone();
		nxtlev.set(Symbol.semicolon);
		nxtlev.set(Symbol.endsym);
		parseStatement(nxtlev, lev);
		// 循环分析{; <语句>}，直到下一个符号不是语句开始符号或收到end
		while (statbegsys.get(sym) || sym == Symbol.semicolon) {
			if (sym == Symbol.semicolon)
				nextSym();
			else
				Err.report(10);					// 缺少分号
			parseStatement(nxtlev, lev);
		}
		if (sym == Symbol.endsym)
			nextSym();
		else
			Err.report(17);						// 缺少end或分号
	}

	/**
	 * 分析<条件语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseIfStatement(SymSet fsys, int lev) {
		int cx1;
		SymSet nxtlev;
		
		nextSym();
		nxtlev = (SymSet) fsys.clone();
		nxtlev.set(Symbol.thensym);				// 后跟符号为then或do ???
		nxtlev.set(Symbol.dosym);
		parseCondition(nxtlev, lev);			// 分析<条件>
		if (sym == Symbol.thensym)
			nextSym();
		else
			Err.report(16);				// 缺少then
		cx1 = interp.cx;						// 保存当前指令地址
		interp.gen(Fct.JPC, 0, 0);			// 生成条件跳转指令，跳转地址未知，暂时写0
		parseStatement(fsys, lev);				// 处理then后的语句
		interp.code[cx1].a = interp.cx;			// 经statement处理后，cx为then后语句执行
												// 完的位置，它正是前面未定的跳转地址
	}

	/**
	 * 分析<过程调用语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseCallStatement(SymSet fsys, int lev) {
		int i;
		nextSym();
		if (sym == Symbol.ident) {
			i = table.position(lex.id);
			if (i == 0) {
				Err.report(11);					// 过程未找到
			} else {
				Table.Item item = table.get(i);
				if (item.kind == Objekt.procedure)
					interp.gen(Fct.CAL, lev - item.level, item.adr);
				else
					Err.report(15);				// call后标识符应为过程
			}
			nextSym();
		} else {
			Err.report(14);						// call后应为标识符
		}
	}

	/**
	 * 分析<写语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseWriteStatement(SymSet fsys, int lev) {
		SymSet nxtlev;

		nextSym();
		if (sym == Symbol.lparen) {
			do {
				nextSym();
				nxtlev = (SymSet) fsys.clone();
				nxtlev.set(Symbol.rparen);
				nxtlev.set(Symbol.comma);
				parseExpression(nxtlev, lev);
				interp.gen(Fct.OPR, 0, 14);
			} while (sym == Symbol.comma);
			
			if (sym == Symbol.rparen)
				nextSym();
			else
				Err.report(33);				// write()中应为完整表达式
		}
		interp.gen(Fct.OPR, 0, 15);
	}

	/**
	 * 分析<读语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseReadStatement(SymSet fsys, int lev) {
		int i;
		
		nextSym();
		if (sym == Symbol.lparen) {
			do {
				nextSym();
				if (sym == Symbol.ident)
					i = table.position(lex.id);
				else
					i = 0;
				
				if (i == 0) {
					Err.report(35);			// read()中应是声明过的变量名
				} else {
					Table.Item item = table.get(i);
					if (item.kind != Objekt.integer) {
						Err.report(32);		// read()中的标识符不是变量, thanks to amd
					} else {
						interp.gen(Fct.OPR, 0, 16);
						interp.gen(Fct.STO, lev-item.level, item.adr);
					}
				}
				
				nextSym();
			} while (sym == Symbol.comma);
		} else {
			Err.report(34);					// 格式错误，应是左括号
		}
		
		if (sym == Symbol.rparen) {
			nextSym();
		} else {
			Err.report(33);					// 格式错误，应是右括号
			while (!fsys.get(sym))
				nextSym();
		}
	}

	/**
	 * 分析<赋值语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseAssignStatement(SymSet fsys, int lev) {
		int i;
		SymSet nxtlev;

		i = table.position(lex.id);
		if (i > 0) {
			Table.Item item = table.get(i);
			if (item.kind == Objekt.integer || item.kind == Objekt.bool) {
				nextSym();
				if (sym == Symbol.becomes)
					nextSym();
				else
					Err.report(13);					// 没有检测到赋值符号
				nxtlev = (SymSet) fsys.clone();
				if(item.kind == Objekt.integer)
					parseExpression(nxtlev, lev);
				else if(item.kind == Objekt.bool)
                    parseBoolExpression(nxtlev, lev);
				else
					Err.report(-6);					// 无法识别的类型
				// parseExpression将产生一系列指令，但最终结果将会保存在栈顶，执行sto命令完成赋值
				interp.gen(Fct.STO, lev - item.level, item.adr);
			} else {
				Err.report(12);						// 赋值语句格式错误
			}
		} else {
			Err.report(11);							// 变量未找到
		}
	}

	/**
	 * 分析<逻辑非因子>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseBoolNotFactor(SymSet fsys, int lev) {
		SymSet nxtlev; short not = 0;
		while(sym == Symbol.not) {
			not ^= 1;
			nextSym();
        }
        nxtlev = (SymSet)fsys.clone();
		if(sym == Symbol.ident || sym == Symbol.lparen) {
			parseBoolFactor(nxtlev, lev);
			if(not == 1)
				interp.gen(Fct.OPR, 0, 17);
		} else
			Err.report(-16);								// 不是合法的逻辑表达式
	}

    /**
     * 分析<逻辑因子>
     * @param fsys 后跟a澳际
     * @param lev
     */
	private void parseBoolFactor(SymSet fsys, int lev) {
	    SymSet nxtlev;
	    nxtlev = (SymSet)fsys.clone();
        if(sym == Symbol.not)
            parseBoolNotFactor(nxtlev, lev);
        else if(sym == Symbol.ident) {
            int idx = table.position(lex.id);
            nextSym();
            if(idx > 0) {
                Table.Item item = table.get(idx);
                if(item.kind == Objekt.bool) {
                    interp.gen(Fct.LOD, lev - item.level, item.adr);
                } else
                    Err.report(-17);					// 标识符类型不匹配
            } else
                Err.report(11);						// 标识符未定义
        }
        else if(sym == Symbol.lparen) {
            nextSym();
            parseBoolExpression(nxtlev, lev);
            if(sym == Symbol.rparen)
                nextSym();
            else
                Err.report(33);						// 缺少右括号
        }
        else
            parseCondition(nxtlev, lev);
    }

	/**
	 * 分析<逻辑项>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseBoolItem(SymSet fsys, int lev) {
	    SymSet nxtlev;
	    nxtlev = (SymSet)fsys.clone();
	    nxtlev.set(Symbol.and);
	    parseBoolFactor(nxtlev, lev);
	    while(sym == Symbol.and) {
	        nextSym();
	        parseBoolFactor(nxtlev, lev);
            interp.gen(Fct.OPR, 0, 19);
        }
	}

	/**
	 * 分析<逻辑表达式>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseBoolExpression(SymSet fsys, int lev) {
		SymSet nxtlev;
		if(sym == Symbol.truesym || sym == Symbol.falsesym) {
			interp.gen(Fct.LIT, 0, sym == Symbol.truesym ? 1 : 0);
			nextSym();
		} else {
			nxtlev = (SymSet)fsys.clone();
			nxtlev.set(Symbol.or);
		    parseBoolItem(nxtlev, lev);
		    while(sym == Symbol.or) {
		    	nextSym();
				parseBoolItem(nxtlev, lev);
				interp.gen(Fct.OPR, 0, 18);
			}
		}
	}

	/**
	 * 分析<表达式>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseExpression(SymSet fsys, int lev) {
		Symbol addop;
		SymSet nxtlev;
		
		// 分析[+|-]<项>
		if (sym == Symbol.plus || sym == Symbol.minus) {
			addop = sym;
			nextSym();
			nxtlev = (SymSet) fsys.clone();
			nxtlev.set(Symbol.plus);
			nxtlev.set(Symbol.minus);
			parseTerm(nxtlev, lev);
			if (addop == Symbol.minus)
				interp.gen(Fct.OPR, 0, 1);
		} else {
			nxtlev = (SymSet) fsys.clone();
			nxtlev.set(Symbol.plus);
			nxtlev.set(Symbol.minus);
			parseTerm(nxtlev, lev);
		}
		
		// 分析{<加法运算符><项>}
		while (sym == Symbol.plus || sym == Symbol.minus) {
			addop = sym;
			nextSym();
			nxtlev = (SymSet) fsys.clone();
			nxtlev.set(Symbol.plus);
			nxtlev.set(Symbol.minus);
			parseTerm(nxtlev, lev);
			if (addop == Symbol.plus)
				interp.gen(Fct.OPR, 0, 2);
			else
				interp.gen(Fct.OPR, 0, 3);
		}
	}

	/**
	 * 分析<项>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseTerm(SymSet fsys, int lev) {
		Symbol mulop;
		SymSet nxtlev;

		// 分析<因子>
		nxtlev = (SymSet) fsys.clone();
		nxtlev.set(Symbol.times);
		nxtlev.set(Symbol.slash);
		parseFactor(nxtlev, lev);
		
		// 分析{<乘法运算符><因子>}
		while (sym == Symbol.times || sym == Symbol.slash) {
			mulop = sym;
			nextSym();
			parseFactor(nxtlev, lev);
			if (mulop == Symbol.times)
				interp.gen(Fct.OPR, 0, 4);
			else
				interp.gen(Fct.OPR, 0, 5);
		}
	}

	/**
	 * 分析<因子>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseFactor(SymSet fsys, int lev) {
		SymSet nxtlev;

		test(facbegsys, fsys, 24);			// 检测因子的开始符号
		// the original while... is problematic: var1(var2+var3)
		// thanks to macross
		// while(inset(sym, facbegsys))
		if (facbegsys.get(sym)) {
			if (sym == Symbol.ident) {			// 因子为常量或变量
				int i = table.position(lex.id);
				if (i > 0) {
					Table.Item item = table.get(i);
					switch (item.kind) {
					case constant:			// 名字为常量
						interp.gen(Fct.LIT, 0, item.val);
						break;
					case bool:
					case integer:
						interp.gen(Fct.LOD, lev - item.level, item.adr);
						break;
					case procedure:			// 名字为过程
						Err.report(21);				// 不能为过程
						break;
					}
				} else {
					Err.report(11);					// 标识符未声明
				}
				nextSym();
			} else if (sym == Symbol.number) {	// 因子为数 
				int num = lex.num;
				if (num > PL0.amax) {
					Err.report(31);
					num = 0;
				}
				interp.gen(Fct.LIT, 0, num);
				nextSym();
			} else if (sym == Symbol.lparen) {	// 因子为表达式
				nextSym();
				nxtlev = (SymSet) fsys.clone();
				nxtlev.set(Symbol.rparen);
				parseExpression(nxtlev, lev);
				if (sym == Symbol.rparen)
					nextSym();
				else
					Err.report(22);					// 缺少右括号
			} else {
				// 做补救措施
				test(fsys, facbegsys, 23);
			}
		}
	}

	/**
	 * 分析<条件>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseCondition(SymSet fsys, int lev) {
		Symbol relop;
		SymSet nxtlev;
		
		if (sym == Symbol.oddsym) {
			// 分析 ODD<表达式>
			nextSym();
			parseExpression(fsys, lev);
			interp.gen(Fct.OPR, 0, 6);
		} else {
			// 分析<表达式><关系运算符><表达式>
			nxtlev = (SymSet) fsys.clone();
			nxtlev.set(Symbol.eql);
			nxtlev.set(Symbol.neq);
			nxtlev.set(Symbol.lss);
			nxtlev.set(Symbol.leq);
			nxtlev.set(Symbol.gtr);
			nxtlev.set(Symbol.geq);
			parseExpression(nxtlev, lev);
			if (sym == Symbol.eql || sym == Symbol.neq 
					|| sym == Symbol.lss || sym == Symbol.leq
					|| sym == Symbol.gtr || sym == Symbol.geq) {
				relop = sym;
				nextSym();
				parseExpression(fsys, lev);
				switch (relop) {
				case eql:
					interp.gen(Fct.OPR, 0, 8);
					break;
				case neq:
					interp.gen(Fct.OPR, 0, 9);
					break;
				case lss:
					interp.gen(Fct.OPR, 0, 10);
					break;
				case geq:
					interp.gen(Fct.OPR, 0, 11);
					break;
				case gtr:
					interp.gen(Fct.OPR, 0, 12);
					break;
				case leq:
					interp.gen(Fct.OPR, 0, 13);
					break;
				}
			} else {
				Err.report(20);
			}
		}
	}
}
