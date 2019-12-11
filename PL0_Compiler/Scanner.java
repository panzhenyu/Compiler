package PL0_Compiler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import DataStructure.DFA;

/**
 *　　词法分析器负责的工作是从源代码里面读取文法符号，这是PL/0编译器的主要组成部分之一。
 */

public class Scanner {
	/**
	 * 刚刚读入的字符
	 */
	private char ch = ' ';
	
	/**
	 * 当前读入的行
	 */
	private char[] line;
	
	/**
	 * 当前行的长度（line length）
	 */
	public int ll = 0;
	
	/**
	 * 当前字符在当前行中的位置（character counter）
	 */
	public int cc = 0;
	
	/**
	 * 当前读入的符号
	 */
	public Symbol sym;
	
	/**
	 * 保留字列表（注意保留字的存放顺序）
	 */
	private String[] word;
	
	/**
	 * 保留字对应的符号值
	 */
	private Symbol[] wsym;
	
	/**
	 * 单字符的符号值
	 */
	private Symbol[] ssym;

	// 输入流
	private BufferedReader in;

	/**
	 * 标识符名字（如果当前符号是标识符的话）
	 * @see Parser
	 * @see Table#enter
	 */
	public String id;

	/**
	 * 数值大小（如果当前符号是数字的话）
	 * @see Parser
	 * @see Table#enter
	 */
	public int num;

	// DFA
	private DFA dfa;
	private final int IDENT, NUMBER, OPERATOR, BLANK, LINEBREAK;

	/**
	 * 初始化词法分析器
	 * @param input PL/0 源文件输入流
	 */

	public Scanner(BufferedReader input) {
		in = input;
		
		// 设置单字符符号
		ssym = new Symbol[256];
		java.util.Arrays.fill(ssym, Symbol.nul);
		ssym['+'] = Symbol.plus;
		ssym['-'] = Symbol.minus;
		ssym['*'] = Symbol.times;
		ssym['/'] = Symbol.slash;
		ssym['('] = Symbol.lparen;
		ssym[')'] = Symbol.rparen;
		ssym['='] = Symbol.eql;
		ssym[','] = Symbol.comma;
		ssym['.'] = Symbol.period;
		ssym['#'] = Symbol.neq;
		ssym[';'] = Symbol.semicolon;
		ssym['!'] = Symbol.not;
		ssym['<'] = Symbol.lss;
		ssym['>'] = Symbol.gtr;
		ssym[':'] = Symbol.colon;
		
		// 设置保留字名字,按照字母顺序，便于折半查找
		word = new String[] {"begin", "boolean", "call", "const", "do", "downto", "end", "false", "for", "if", "integer",
			"odd", "procedure", "read", "then", "to", "true", "var", "while", "write"};
		
		// 设置保留字符号
		int i = 0;
		wsym = new Symbol[PL0.norw];
		wsym[i++] = Symbol.beginsym;
		wsym[i++] = Symbol.booleansym;
		wsym[i++] = Symbol.callsym;
		wsym[i++] = Symbol.constsym;
		wsym[i++] = Symbol.dosym;
		wsym[i++] = Symbol.downtosym;
		wsym[i++] = Symbol.endsym;
		wsym[i++] = Symbol.falsesym;
		wsym[i++] = Symbol.forsym;
		wsym[i++] = Symbol.ifsym;
		wsym[i++] = Symbol.integersym;
		wsym[i++] = Symbol.oddsym;
		wsym[i++] = Symbol.procsym;
		wsym[i++] = Symbol.readsym;
		wsym[i++] = Symbol.thensym;
		wsym[i++] = Symbol.tosym;
		wsym[i++] = Symbol.truesym;
		wsym[i++] = Symbol.varsym;
		wsym[i++] = Symbol.whilesym;
		wsym[i++] = Symbol.writesym;

		IDENT = -2; NUMBER = -3; OPERATOR = -4; BLANK = -5; LINEBREAK = -6;
		dfa = new DFA(
				new String[] {"(_|\\c)(\\w|_)*", "\\d+", "\\+|-|\\*|/|\\(|\\)|=|.|,|#|;|<=|>=|:=|<|>|\\|\\||&&|!|:", " |\\t", "\\n"},
				new int[] {IDENT, NUMBER, OPERATOR, BLANK, LINEBREAK}
		);
	}

	/**
	 * 读取一个字符，为减少磁盘I/O次数，每次读取一行
	 */
	void getch() {
		String l = "";
		try {
			if (cc == ll) {
				while (l.equals(""))
					l = in.readLine().toLowerCase() + "\n";
				ll = l.length();
				cc = 0;
				line = l.toCharArray();
//				System.out.println(PL0.interp.cx + " " + l);
//				PL0.fa1.println(PL0.interp.cx + " " + l);
			}
		} catch (IOException e) {
			throw new Error("program imcomplete");
		}
		ch = line[cc];
		cc ++;
	}
	
	public void getsym() {
		StringBuilder s = new StringBuilder(); int end_type = -1;
		dfa.reset();
		while(true) {
			dfa.action(ch);
			if(dfa.isEndingState()) {
				end_type = dfa.getEndingType();
				if(end_type == BLANK || end_type == LINEBREAK) {
					dfa.reset();
					getch();
					continue;
				}
			}
			if(dfa.isLegalState()) {
				s.append(ch);
				getch();
			} else break;
		}
		record(s, end_type);
	}

	private void record(StringBuilder s, int end_type)
	{
		if(end_type == -1)
			throw new RuntimeException("illegal character " + ch);
		int i;
		if(end_type == IDENT) {
			id = s.toString();
			i = java.util.Arrays.binarySearch(word, id);
			if (i < 0) {
				// 一般标识符
				sym = Symbol.ident;
			} else {
				// 关键字
				sym = wsym[i];
			}
		} else if(end_type == NUMBER) {
			sym = Symbol.number;
			num = 0; i = 0;
			while(i<s.length()) {
				num = 10*num + Character.digit(s.charAt(i), 10);
				i++;
			}
			i--;
			if (i > PL0.nmax)
				Err.report(30);
		} else if(end_type == OPERATOR) {
		    String ops = s.toString();
		    switch (ops) {
				case ":=": sym = Symbol.becomes; break;
				case "<=": sym = Symbol.leq; break;
				case ">=": sym = Symbol.geq; break;
				case "||": sym = Symbol.or; break;
				case "&&": sym = Symbol.and; break;
				default:
					sym = ssym[ops.charAt(0)];
					break;
			}
		}
	}

	public static void main(String[] args) throws IOException
    {
        BufferedReader in = new BufferedReader(new FileReader("proc1.txt"));
        Scanner s = new Scanner(in);
        while(s.sym != Symbol.period) {
            s.getsym();
            System.out.print(s.sym + " ");
        }
    }
	/**
	 * 分析关键字或者一般标识符
	 */
	void matchKeywordOrIdentifier() {
		int i;
        StringBuilder sb = new StringBuilder(PL0.al);
		// 首先把整个单词读出来
		do {
			sb.append(ch);
			getch();
		} while (ch >= 'a' && ch <= 'z' || ch >= '0' && ch <= '9');
		id = sb.toString();
		
		// 然后搜索是不是保留字（请注意使用的是什么搜索方法）
		i = java.util.Arrays.binarySearch(word, id);
		
		// 最后形成符号信息
		if (i < 0) {
			// 一般标识符
			sym = Symbol.ident;
		} else {
			// 关键字
			sym = wsym[i];
		}
	}
	
	/**
	 * 分析数字
	 */
	void matchNumber() {
		int k = 0;
		sym = Symbol.number;
		num = 0;
		do {
			num = 10*num + Character.digit(ch, 10);
			k++;
			getch();
		} while (ch>='0' && ch<='9'); 				// 获取数字的值
		k--;
		if (k > PL0.nmax)
			Err.report(30);
	}
	
	/**
	 * 分析操作符
	 */
	void matchOperator() {
		// 请注意这里的写法跟Wirth的有点不同
		switch (ch) {
		case ':':		// 赋值符号
			getch();
			if (ch == '=') {
				sym = Symbol.becomes;
				getch();
			} else {
				// 不能识别的符号
				sym = Symbol.nul;
			}
			break;
		case '<':		// 小于或者小于等于
			getch();
			if (ch == '=') {
				sym = Symbol.leq;
				getch();
			} else {
				sym = Symbol.lss;
			}
			break;
		case '>':		// 大于或者大于等于
			getch();
			if (ch == '=') {
				sym = Symbol.geq;
				getch();
			} else {
				sym = Symbol.gtr;
			}
			break;
		default:		// 其他为单字符操作符（如果符号非法则返回nil）
			sym = ssym[ch];
			if (sym != Symbol.period)
				getch();
			break;
		}
	}	
}
