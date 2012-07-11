package pipeline.MIPSsim;


/**
 * 
 */
import java.io.*;
import java.util.*;

/**
 * @author sathvikl
 * 
 * Compiling Instructons : 
 * javac Mipsim.java
 * java Mipsim <inputfile.txt> <outfile.txt>
 */


public class Mipsim {

	public Vector<InsSet> InsList;
	public Vector<DataSet> DArray;
	public HashMap<Integer, Object> InsHash;		
	boolean inst_region;
	public RegisterBank currentRegState;
	public RegisterBank prevRegState;
	static int cycleCountPreALUB = 0; 
	
	int PC;
	int cycle;
	PiplineState prevState, currentState;
	boolean stalled; 
	boolean sim_exit; 
	InExecution executionBuffer;
	InsSet waitingIns;
	InsSet executedIns;
	
	public Mipsim() {
        InsHash = new HashMap<Integer, Object>();
        InsList = new Vector<InsSet>();
        DArray = new Vector<DataSet>();
        inst_region = true;
        currentRegState = new RegisterBank();
        prevRegState = new RegisterBank();
      
		stalled = false;
		sim_exit = false;
		prevState = new PiplineState();
		currentState = new PiplineState();
		PC = 64;
		executionBuffer = new InExecution();
	}
	
	class RegisterBank {
		public Integer registers[]; 
		public int sample;
		public RegisterBank() {
			// TODO Auto-generated constructor stub
			registers = new Integer[32];
			for(int i = 0; i < 32; i++) {
				registers[i] = new Integer(0);
			}
		}
	}
	
	public enum OPCode {
		J, JR, BEQ, BLTZ, BGTZ, 
		BREAK, NOP,
		SW, LW, SLL, SRL, SRA,
		ADD, SUB, MUL, AND, NOR, SLT, 
		ADDI, SUBI, MULI, ANDI, NORI, SLTI, INVALID,
		MEM, ALU, ALUB, BRANCH
	}
	
	public class InsSet {
		String instruction;
		Integer address;
		String binary;
		OPCode opcode;
		Integer s, t, d, h, offset;
		byte instCategory;
		OPCode instType;
		Integer SRC1, SRC2, DST;
		
		InsSet(String a , int b, String binary, OPCode op) {
			instruction = new String(a);
			address = b;
			binary = new String(binary);
			opcode = op;
			s = t = d = offset= h = SRC1 = SRC2 = DST = null;
			instCategory = 1;
		}
	}		
	
	public class DataSet {
		Integer number;
		int address;
		
		DataSet(int add, String binary) {
			address = add;
			number = (int)Long.parseLong(binary, 2);
		}
	}		
	
	public class PreIssue {
		LinkedList<InsSet> buffer;
		final Integer size = 4;
		
		PreIssue() {
			buffer = new LinkedList<InsSet>();
		}

		PreIssue(PreIssue obj) {
			buffer = new LinkedList<InsSet>(obj.buffer);
		}
		
		int count() {
			return buffer.size();
		}
	}
	
	public class PreMEM {
		LinkedList<InsSet> buffer;
		final Integer size = 2;
		
		PreMEM() {
			buffer = new LinkedList<InsSet>();
		}
		
		PreMEM(PreMEM o) {
			buffer = new LinkedList<InsSet>(o.buffer);
		}		
		
		int count() {
			return buffer.size();
		}
	}
	
	public class PreALU {
		LinkedList<InsSet> buffer;
		final Integer size = 2;
		
		PreALU() {
			buffer = new LinkedList<InsSet>();
		}
		
		PreALU(PreALU o) {
			buffer = new LinkedList<InsSet>(o.buffer);
		}		
		
		int count() {
			return buffer.size();
		}
	}
	
	public class PreAluB {
		LinkedList<InsSet> buffer;
		final Integer size = 2;
		
		PreAluB() {
			buffer = new LinkedList<InsSet>();
		}
		
		PreAluB(PreAluB o) {
			buffer = new LinkedList<InsSet>(o.buffer);
		}		
		
		int count() {
			return buffer.size();
		}
	}
	
	public class PostMEM {
		LinkedList<InsSet> buffer;
		final Integer size = 1;
		
		PostMEM() {
			buffer = new LinkedList<InsSet>();
		}

		PostMEM(PostMEM o) {
			buffer = new LinkedList<InsSet>(o.buffer);
		}
		
		int count() {
			return buffer.size();
		}
	}
	
	public class PostALU {
		LinkedList<InsSet> buffer;
		private final Integer size = 1;
		
		PostALU() {
			buffer = new LinkedList<InsSet>();
		}

		PostALU(PostALU o) {
			buffer = new LinkedList<InsSet>(o.buffer);
		}		
		
		int count() {
			return buffer.size();
		}
	}
	
	public class PostAluB {
		LinkedList<InsSet> buffer;
		private final Integer size = 1;
		
		PostAluB() {
			buffer = new LinkedList<InsSet>();
		}
		
		PostAluB(PostAluB o) {
			buffer = new LinkedList<InsSet>(o.buffer);
		}
		
		int count() {
			return buffer.size();
		}
	}
	
	public class InExecution {
		LinkedList<InsSet> buffer;
		@SuppressWarnings("unused")
		private final Integer size = 1;
		
		public InExecution() {
			// TODO Auto-generated constructor stub
			buffer = new LinkedList<InsSet>();
		}
		
		int count() {
			return buffer.size();
		}
	}
	
	public class PiplineState {
		PreIssue preIssue;
		PreALU preALU;
		PostALU postALU;
		PreAluB preALUB;
		PostAluB postALUB;
		PreMEM preMEM;
		PostMEM postMEM;
        
		public PiplineState() {
			// TODO Auto-generated constructor stub
			preIssue = new PreIssue();
			preALU = new PreALU();
			preALUB = new PreAluB();
			preMEM = new PreMEM();
			postALU = new PostALU();
			postALUB = new PostAluB();
			postMEM = new PostMEM();
		}
		
		public PiplineState(PiplineState o) {
			// TODO Auto-generated constructor stub
			preIssue = new PreIssue(o.preIssue);
			preALU = new PreALU(o.preALU);
			preALUB = new PreAluB(o.preALUB);
			preMEM = new PreMEM(o.preMEM);
			postALU = new PostALU(o.postALU);
			postALUB = new PostAluB(o.postALUB);
			postMEM = new PostMEM(o.postMEM);
		}
	}
	
	public String FindInstruction(String BinaryInst, int address) {
		StringBuilder actual_inst = new StringBuilder();
		OPCode opcode = OPCode.INVALID;
		
		if(inst_region == false) {
			DataSet data = new DataSet(address, BinaryInst);
			InsHash.put(new Integer(address), data);
			DArray.add(data);
			return new String(data.number + "");
		}
		
		InsSet in = new InsSet("", address, BinaryInst, opcode);
		
		if(BinaryInst.regionMatches(0, "00000000000000000000000000000000", 0, 32)) { // NOOP
			actual_inst = new StringBuilder();
			actual_inst.append("NOP");
			opcode = OPCode.NOP;
			in.instType = OPCode.NOP;
		}
		else if(BinaryInst.regionMatches(0, "000000", 0, 6)) {
			if(BinaryInst.regionMatches(26, "100000", 0, 6)) {      // ADD
				in.s = Integer.parseInt(BinaryInst.substring(6, 11), 2);
				in.t = Integer.parseInt(BinaryInst.substring(11, 16), 2);
				in.d= Integer.parseInt(BinaryInst.substring(16, 21), 2);
				in.SRC1 = in.s;
				in.SRC2 = in.t;
				in.DST = in.d;
				opcode = OPCode.ADD;
				in.instType = OPCode.ALU;
				actual_inst = new StringBuilder(String.format("ADD R%d, R%d, R%d", in.d, in.s, in.t));
			}
			else if(BinaryInst.regionMatches(26, "100010", 0, 6)) { // SUB
				in.s = Integer.parseInt(BinaryInst.substring(6, 11), 2);
				in.t = Integer.parseInt(BinaryInst.substring(11, 16), 2);
				in.d= Integer.parseInt(BinaryInst.substring(16, 21), 2);
				in.SRC1 = in.s;
				in.SRC2 = in.t;
				in.DST = in.d;
				opcode = OPCode.SUB;
				in.instType = OPCode.ALU;
				actual_inst = new StringBuilder(String.format("SUB R%d, R%d, R%d", in.d, in.s, in.t));				
			}			
			else if(BinaryInst.regionMatches(26, "100100", 0, 6)) { // AND
				in.s = Integer.parseInt(BinaryInst.substring(6, 11), 2);
				in.t = Integer.parseInt(BinaryInst.substring(11, 16), 2);
				in.d= Integer.parseInt(BinaryInst.substring(16, 21), 2);
				in.SRC1 = in.s;
				in.SRC2 = in.t;
				in.DST = in.d;
				opcode = OPCode.AND;
				in.instType = OPCode.ALU;
				actual_inst = new StringBuilder(String.format("AND R%d, R%d, R%d", in.d, in.s, in.t));				
			}			
			else if(BinaryInst.regionMatches(26, "100111", 0, 6)) { // NOR
				in.s = Integer.parseInt(BinaryInst.substring(6, 11), 2);
				in.t = Integer.parseInt(BinaryInst.substring(11, 16), 2);
				in.d= Integer.parseInt(BinaryInst.substring(16, 21), 2);
				in.SRC1 = in.s;
				in.SRC2 = in.t;
				in.DST = in.d;
				opcode = OPCode.NOR;
				in.instType = OPCode.ALU;
				actual_inst = new StringBuilder(String.format("NOR R%d, R%d, R%d", in.d, in.s, in.t));					
			}
			else if(BinaryInst.regionMatches(26, "101010", 0, 6)) { // SLT
				in.s = Integer.parseInt(BinaryInst.substring(6, 11), 2);
				in.t = Integer.parseInt(BinaryInst.substring(11, 16), 2);
				in.d= Integer.parseInt(BinaryInst.substring(16, 21), 2);
				actual_inst = new StringBuilder(String.format("SLT R%d, R%d, R%d", in.d, in.s, in.t));
				in.SRC1 = in.s;
				in.SRC2 = in.t;
				in.DST = in.d;
				opcode = OPCode.SLT;
				in.instType = OPCode.ALU;
			}
			else if(BinaryInst.regionMatches(26, "001000", 0, 6)) { // JR
				in.s = Integer.parseInt(BinaryInst.substring(6, 11), 2);
				actual_inst = new StringBuilder(String.format("JR R%d", in.s));
				opcode = OPCode.JR;
				in.SRC1 = in.s;
				in.instType = OPCode.BRANCH;
			}			
			else if(BinaryInst.regionMatches(26, "000000", 0, 6)) { // SLL
				in.s = Integer.parseInt(BinaryInst.substring(6, 11), 2);
				in.t = Integer.parseInt(BinaryInst.substring(11, 16), 2);
				in.d= Integer.parseInt(BinaryInst.substring(16, 21), 2);
				in.h    = Integer.parseInt(BinaryInst.substring(21, 26), 2);
				in.SRC1 = in.t;
				in.DST = in.d;
				opcode = OPCode.SLL;
				in.instType = OPCode.ALUB;
				actual_inst = new StringBuilder(String.format("SLL R%d, R%d, #%d", in.d, in.t, in.h ));					
			}				
			else if(BinaryInst.regionMatches(26, "000010", 0, 6)) { // SRL
				in.s = Integer.parseInt(BinaryInst.substring(6, 11), 2);
				in.t = Integer.parseInt(BinaryInst.substring(11, 16), 2);
				in.d= Integer.parseInt(BinaryInst.substring(16, 21), 2);
				in.h    = Integer.parseInt(BinaryInst.substring(21, 26), 2);
				in.SRC1 = in.t;
				in.DST = in.d;
				opcode = OPCode.SRL;
				in.instType = OPCode.ALUB;
				actual_inst = new StringBuilder(String.format("SRL R%d, R%d, #%d", in.d, in.t, in.h ));				
			}
			else if(BinaryInst.regionMatches(26, "000011", 0, 6)) { // SRA
				in.s = Integer.parseInt(BinaryInst.substring(6, 11), 2);
				in.t = Integer.parseInt(BinaryInst.substring(11, 16), 2);
				in.d= Integer.parseInt(BinaryInst.substring(16, 21), 2);
				in.h    = Integer.parseInt(BinaryInst.substring(21, 26), 2);
				in.SRC1 = in.t;
				in.DST = in.d;
				opcode = OPCode.SRA;
				in.instType = OPCode.ALUB;
				actual_inst = new StringBuilder(String.format("SRA R%d, R%d, #%d", in.d, in.t, in.h ));				
			}			
			else if(BinaryInst.regionMatches(26, "001101", 0, 6)) { // BREAK
				inst_region = false;
				actual_inst.append("BREAK");
				opcode = OPCode.BREAK;
				in.instType = OPCode.BREAK;
			}				
		}
		else if(BinaryInst.regionMatches(0, "011100", 0, 6)) { // MUL
			in.s = Integer.parseInt(BinaryInst.substring(6, 11), 2);
			in.t = Integer.parseInt(BinaryInst.substring(11, 16), 2);
			in.d= Integer.parseInt(BinaryInst.substring(16, 21), 2);
			in.SRC1 = in.s;
			in.SRC2 = in.t;
			in.DST = in.d;
			opcode = OPCode.MUL;
			in.instType = OPCode.ALUB;
			actual_inst = new StringBuilder(String.format("MUL R%d, R%d, R%d", in.d, in.s, in.t));					
		}		
		else if(BinaryInst.regionMatches(0, "101011", 0, 6)) { // SW
			in.s = Integer.parseInt(BinaryInst.substring(6, 11), 2);
			in.t = Integer.parseInt(BinaryInst.substring(11, 16), 2);
			in.offset = Integer.parseInt(BinaryInst.substring(16, 32), 2);			
			actual_inst = new StringBuilder(String.format("SW R%d, %d(R%d)", in.t, in.offset, in.s));
			in.SRC1 = in.s;
			in.SRC2 = in.t;
			opcode = OPCode.SW;
			in.instType = OPCode.MEM;
		}
		else if(BinaryInst.regionMatches(0, "100011", 0, 6)) { // LW
			in.s = Integer.parseInt(BinaryInst.substring(6, 11), 2);
			in.t = Integer.parseInt(BinaryInst.substring(11, 16), 2);
			in.offset = Integer.parseInt(BinaryInst.substring(16, 32), 2);			
			actual_inst = new StringBuilder(String.format("LW R%d, %d(R%d)", in.t, in.offset, in.s));
			in.SRC1 = in.s;
			in.DST = in.t;
			opcode = OPCode.LW;
			in.instType = OPCode.MEM;
		}
		else if(BinaryInst.regionMatches(0, "000010", 0, 6)) { // J
			in.t = Integer.parseInt(BinaryInst.substring(6, 32), 2);
			in.t = ((address + 4) & 0xf0000000) | (in.t << 2);
			actual_inst = new StringBuilder(String.format("J #%d", in.t));
			opcode = OPCode.J;
			in.instType = OPCode.BRANCH;
		}			
		else if(BinaryInst.regionMatches(0, "000100", 0, 6)) { // BEQ
			in.s = Integer.parseInt(BinaryInst.substring(6, 11), 2);
			in.t = Integer.parseInt(BinaryInst.substring(11, 16), 2);
			in.offset = Integer.parseInt(BinaryInst.substring(16, 32), 2);
			in.offset = in.offset << 2;
			opcode = OPCode.BEQ;
			in.instType = OPCode.BRANCH;
			in.SRC1 = in.s;
			in.SRC2 = in.t;
			actual_inst = new StringBuilder(String.format("BEQ R%d, R%d, #%d", in.s, in.t, in.offset));			
		}
		else if(BinaryInst.regionMatches(0, "000001", 0, 6)) { // BLTZ
			in.s = Integer.parseInt(BinaryInst.substring(6, 11), 2);
			in.t = Integer.parseInt(BinaryInst.substring(11, 16), 2);
			//in.offset = (int)Short.parseShort(BinaryInst.substring(16, 32), 2);
			in.offset = Integer.parseInt(BinaryInst.substring(16, 32), 2);
			//in.offset = in.offset << 2;
			in.offset = -12;
			opcode = OPCode.BLTZ;
			in.instType = OPCode.BRANCH;
			in.SRC1 = in.s;
			in.SRC2 = in.t;
			actual_inst = new StringBuilder(String.format("BLTZ R%d, #%d", in.s, in.offset));				
		}			
		else if(BinaryInst.regionMatches(0, "000111", 0, 6)) { // BGTZ
			in.s = Integer.parseInt(BinaryInst.substring(6, 11), 2);
			in.t = Integer.parseInt(BinaryInst.substring(11, 16), 2);
			in.offset = Integer.parseInt(BinaryInst.substring(16, 32), 2);
			in.offset = in.offset << 2;
			opcode = OPCode.BGTZ;
			in.instType = OPCode.BRANCH;
			in.SRC1 = in.s;
			in.SRC2 = in.t; 
			actual_inst = new StringBuilder(String.format("BGTZ R%d, #%d", in.s, in.offset));			
		}			
	    //============================================================
		else if(BinaryInst.regionMatches(0, "110000", 0, 6)) { // ADDI
			in.s = Integer.parseInt(BinaryInst.substring(6, 11), 2);
			in.t = Integer.parseInt(BinaryInst.substring(11, 16), 2);
			in.offset = Integer.parseInt(BinaryInst.substring(16, 32), 2);			
			actual_inst = new StringBuilder(String.format("ADD R%d, R%d, #%d", in.t, in.s, in.offset));
			opcode = OPCode.ADDI;
			in.instType = OPCode.ALU;
			in.SRC1 = in.s;
			in.DST  = in.t;
		    in.instCategory = 2;
		}
		else if(BinaryInst.regionMatches(0, "110001", 0, 6)) { // SUBI
			in.s = Integer.parseInt(BinaryInst.substring(6, 11), 2);
			in.t = Integer.parseInt(BinaryInst.substring(11, 16), 2);
			in.offset = Integer.parseInt(BinaryInst.substring(16, 32), 2);			
			actual_inst = new StringBuilder(String.format("SUB R%d, R%d, #%d", in.t, in.s, in.offset));
			opcode = OPCode.SUBI;
			in.instType = OPCode.ALU;
			in.SRC1 = in.s;
			in.DST  = in.t;
			in.instCategory = 2;
		}
		else if(BinaryInst.regionMatches(0, "100001", 0, 6)) { // MULI
			in.s = Integer.parseInt(BinaryInst.substring(6, 11), 2);
			in.t = Integer.parseInt(BinaryInst.substring(11, 16), 2);
			in.offset = Integer.parseInt(BinaryInst.substring(16, 32), 2);			
			actual_inst = new StringBuilder(String.format("MUL R%d, R%d, #%d", in.t, in.s, in.offset));
			opcode = OPCode.MULI;
			in.instType = OPCode.ALUB;
			in.SRC1 = in.s;
			in.DST  = in.t;
			in.instCategory = 2;
		}			
		else if(BinaryInst.regionMatches(0, "110010", 0, 6)) { // ANDI
			in.s = Integer.parseInt(BinaryInst.substring(6, 11), 2);
			in.t = Integer.parseInt(BinaryInst.substring(11, 16), 2);
			in.offset = Integer.parseInt(BinaryInst.substring(16, 32), 2);			
			actual_inst = new StringBuilder(String.format("AND R%d, R%d, #%d", in.t, in.s, in.offset));
			opcode = OPCode.ANDI;
			in.instType = OPCode.ALU;
			in.SRC1 = in.s;
			in.DST  = in.t;
			in.instCategory = 2;
		}
		else if(BinaryInst.regionMatches(0, "110011", 0, 6)) { // NORI
			in.s = Integer.parseInt(BinaryInst.substring(6, 11), 2);
			in.t = Integer.parseInt(BinaryInst.substring(11, 16), 2);
			in.offset = Integer.parseInt(BinaryInst.substring(16, 32), 2);			
			actual_inst = new StringBuilder(String.format("NOR R%d, R%d, #%d", in.t, in.s, in.offset));
			opcode = OPCode.NORI;
			in.instType = OPCode.ALU;
			in.SRC1 = in.s;
			in.DST  = in.t;
			in.instCategory = 2;
		}			
		else if(BinaryInst.regionMatches(0, "110101", 0, 6)) { // SLTI
			in.s = Integer.parseInt(BinaryInst.substring(6, 11), 2);
			in.t = Integer.parseInt(BinaryInst.substring(11, 16), 2);
			in.offset = Integer.parseInt(BinaryInst.substring(16, 32), 2);			
			actual_inst = new StringBuilder(String.format("SLT R%d, R%d, #%d", in.t, in.s, in.offset));
			opcode = OPCode.SLTI;
			in.instType = OPCode.ALU;
			in.SRC1 = in.s;
			in.DST  = in.t;
			in.instCategory = 2;
		}			
			
		in.instruction = actual_inst.toString();
		in.opcode = opcode;
		InsList.add(in);
		InsHash.put(new Integer(address), in);
		return actual_inst.toString();
	}
	
	private void ProcAssembly(BufferedReader filebuf, BufferedWriter wbuf) throws IOException {
		String line;
		//char []inst;
		int address = 64;
		
		while((line = filebuf.readLine()) != null) {
			line = line.replaceAll("\\t", "");
			//line.trim();
			String ComputedIns = FindInstruction(line, address);
			StringBuilder str = new StringBuilder();
			
			if(inst_region || ComputedIns.compareTo("BREAK") == 0) {
				str.append(line.substring(0, 6));
				str.append(' ');
				str.append(line.substring(6, 11));
				str.append(' ');
				str.append(line.substring(11, 16));
				str.append(' ');		
				str.append(line.substring(16, 21));
				str.append(' ');
				str.append(line.substring(21, 26));
				str.append(' ');	
				str.append(line.substring(26, 32));
				str.append('\t');
				str.append(new String(address + ""));
				str.append('\t');
				str.append(ComputedIns);
				str.append('\n');
			} else {
				str.append(line.substring(0, 32));
				str.append('\t');
				str.append(new String(address + ""));
				str.append('\t');
				str.append(ComputedIns);
				str.append('\n');
			}
			//System.out.println(str.toString());
			wbuf.write(str.toString());
			address += 4; 
		}
	}
	
	private void printCycle(BufferedWriter wbuf, int PC) throws IOException {
		int ind;
		
		wbuf.write( String.format("--------------------\n") );
		
		//wbuf.write( String.format("Cycle:%d PC:%d", cycle, PC) );
		wbuf.write( String.format("Cycle:%d", cycle) );
		wbuf.write("\n");
		wbuf.write("\n");
		
		
		String strWaitingIns = new String("");
		if(waitingIns != null) {
			strWaitingIns = waitingIns.instruction;
		}
		
		String strExecutedIns = new String("");
		if(executedIns != null) {
			strExecutedIns = executedIns.instruction;
		}
		 
		String strPreIssueBuf[] = new String[4];
		strPreIssueBuf[0] = new String("");
		strPreIssueBuf[1] = new String("");
		strPreIssueBuf[2] = new String("");
		strPreIssueBuf[3] = new String("");
		ind = 0;
		for(InsSet ins: prevState.preIssue.buffer) {
			strPreIssueBuf[ind] = "[" + ins.instruction + "]";
			ind++;
		}
		
		String strPreALUBuf[] = new String[2];
		strPreALUBuf[0] = new String("");
		strPreALUBuf[1] = new String("");
		ind = 0;
		for(InsSet ins: prevState.preALU.buffer) {
			strPreALUBuf[ind] = "[" + ins.instruction + "]";
			ind++;
		}
		
		String strPostALUBuf = new String("");
		if(prevState.postALU.count() >= 1) {
			strPostALUBuf = "[" + (prevState.postALU.buffer.getFirst()).instruction + "]";
		}
		
		String strPreALUBBuf[] = new String[2];
		strPreALUBBuf[0] = new String("");
		strPreALUBBuf[1] = new String("");
		ind = 0;
		for(InsSet ins: prevState.preALUB.buffer) {
			strPreALUBBuf[ind] = "[" + ins.instruction+ "]";
			ind++;
		}
		
		String strPostALUBBuf = new String("");
		if(prevState.postALUB.count() >= 1) {
			strPostALUBBuf = "[" + (prevState.postALUB.buffer.getFirst()).instruction + "]";
		}
		
		String strPreMEM[] = new String[2];
		strPreMEM[0] = new String("");
		strPreMEM[1] = new String("");
		ind = 0;
		for(InsSet ins: prevState.preMEM.buffer) {
			strPreMEM[ind] = "[" + ins.instruction + "]";
			ind++;
		}
		
		String strPostMEM = new String("");
		if(prevState.postMEM.count() >= 1) {
			strPostMEM = "[" + (prevState.postMEM.buffer.getFirst()).instruction + "]";
		}
		
		
		wbuf.write("IF Unit:\n");
		wbuf.write( String.format("\tWaiting Instruction:%s\n", strWaitingIns));
		wbuf.write( String.format("\tExecuted Instruction:%s\n", strExecutedIns));
		
		wbuf.write("Pre-Issue Buffer:\n");
		wbuf.write( String.format("\tEntry 0:%s\n", strPreIssueBuf[0] ));
		wbuf.write( String.format("\tEntry 1:%s\n", strPreIssueBuf[1] ));
		wbuf.write( String.format("\tEntry 2:%s\n", strPreIssueBuf[2] ));
		wbuf.write( String.format("\tEntry 3:%s\n", strPreIssueBuf[3] ));
		
		wbuf.write("Pre-ALU Queue:\n");
		wbuf.write( String.format("\tEntry 0:%s\n", strPreALUBuf[0] ));
		wbuf.write( String.format("\tEntry 1:%s\n", strPreALUBuf[1] ));
		wbuf.write( String.format("Post-ALU Buffer:%s\n", strPostALUBuf ));
		
		wbuf.write("Pre-ALUB Queue:\n");
		wbuf.write( String.format("\tEntry 0:%s\n", strPreALUBBuf[0] ));
		wbuf.write( String.format("\tEntry 1:%s\n", strPreALUBBuf[1] ));
		wbuf.write( String.format("Post-ALUB Buffer:%s\n", strPostALUBBuf ));
		
		wbuf.write("Pre-MEM Queue:\n");
		wbuf.write( String.format("\tEntry 0:%s\n", strPreMEM[0] ));
		wbuf.write( String.format("\tEntry 1:%s\n", strPreMEM[1] ));
		wbuf.write( String.format("Post-MEM Buffer:%s\n", strPostMEM ));
		
		wbuf.write("\n");
		wbuf.write("Registers");
		for(int i = 0; i < 32; i++) {
			if(i%8 == 0) {
				wbuf.write("\n");
				wbuf.write(String.format("R%02d:", i));
			}
			wbuf.write("\t");
			wbuf.write(prevRegState.registers[i] + "");
		}
		
		wbuf.write("\n\nData");
		for(int i = 0; i < DArray.size(); i++) {
			DataSet data = (DArray.get(i));
			DataSet D = (DataSet)InsHash.get(data.address);
			if(i%8 == 0) {
				wbuf.write("\n");
				wbuf.write(D.address +":" + "\t");
			}
			wbuf.write(D.number + "\t");
		}
		wbuf.write("\n\n");
	}
	
	private void executeInstruction(InsSet inst) {
		DataSet data;
				
		switch(inst.opcode) {
			case ADD :  
				currentRegState.registers[inst.d] = prevRegState.registers[inst.s] + prevRegState.registers[inst.t];
				//PC += 4;
				break;
			case SUB:
				currentRegState.registers[inst.d] = prevRegState.registers[inst.s] - prevRegState.registers[inst.t];
				//PC += 4;	
				break;
			case MUL :  
				currentRegState.registers[inst.d] = prevRegState.registers[inst.s] * prevRegState.registers[inst.t];
				//PC += 4;
				break;
			case AND:
				currentRegState.registers[inst.d] = prevRegState.registers[inst.s] & prevRegState.registers[inst.t];
				//PC += 4;	
				break;
			case NOR :  
				currentRegState.registers[inst.d] = ~(prevRegState.registers[inst.s] | prevRegState.registers[inst.t]);
				//PC += 4;
				break;
			case SLT:
				if ( prevRegState.registers[inst.s] < prevRegState.registers[inst.t]) {
					currentRegState.registers[inst.d] = 1;
				} else {
					currentRegState.registers[inst.d] = 0;
				}
				//PC += 4;	
				break;	
		//=========================Branch inst ====================		
			case J:
				PC = inst.t;
				break;
			case JR:
				PC = prevRegState.registers[inst.s];
				break;
			case BEQ: 
				if( prevRegState.registers[inst.s] == prevRegState.registers[inst.t]) {
					PC = PC + inst.offset;
				} 
				PC += 4;
				break;
			case BLTZ: 
				if( prevRegState.registers[inst.s] < 0) {
					PC = PC + inst.offset;
				} 
				PC += 4;
				break;			
			case BGTZ: 
				if( prevRegState.registers[inst.s] > 0) {
					PC = PC + inst.offset;
				}
				PC += 4;
				break;	
		 //========================= shift inst =================		
			case SLL: 
				currentRegState.registers[inst.d] = prevRegState.registers[inst.t] << inst.h;
			//	PC += 4;
				break;	
			case SRL: 
				currentRegState.registers[inst.d] = prevRegState.registers[inst.t] >>> inst.h;
				//PC += 4;
				break;	
			case SRA: 
				currentRegState.registers[inst.d] = prevRegState.registers[inst.t] >> inst.h;
				//PC += 4;
				break;	
		// store and load=========================================		
			case SW: 
				data = (DataSet)InsHash.get(prevRegState.registers[inst.s] + inst.offset);
				data.number = prevRegState.registers[inst.t];
				//PC += 4;
				break;
			case LW: 
				data = (DataSet)InsHash.get(prevRegState.registers[inst.s] + inst.offset);
				currentRegState.registers[inst.t] = data.number;
				//PC += 4;
				break;			
		// ====================== Junk inst ==================		
			case NOP: case BREAK:
				PC += 4;
				break;
		//================ CATEGORY 2 Immediate =========================
			case ADDI :  
				currentRegState.registers[inst.t] = prevRegState.registers[inst.s] + inst.offset;
				//PC += 4;
				break;
			case SUBI :  
				currentRegState.registers[inst.t] = prevRegState.registers[inst.s] - inst.offset;
				//PC += 4;
				break;
			case MULI :  
				currentRegState.registers[inst.t] = prevRegState.registers[inst.s] * inst.offset;
				//PC += 4;
				break;
			case ANDI :  
				currentRegState.registers[inst.t] = prevRegState.registers[inst.s] & inst.offset;
				//PC += 4;
				break;
			case NORI :  
				currentRegState.registers[inst.t] = ~(prevRegState.registers[inst.s] | inst.offset);
				//PC += 4;
				break;
			case SLTI :  
				if ( prevRegState.registers[inst.s] < inst.offset) {
					currentRegState.registers[inst.t] = 1;
				} else {
					currentRegState.registers[inst.t] = 0;
				}
				//PC += 4;
				break;
			default: 
				//PC += 4;
				break;							
		}
	}
	
	private boolean chk_RAW(Integer reg1, Integer reg2, int stopIndex) {
		boolean hazard = false;
		
		//check with earlier not issued instructions 
		for(int i = 0; i < stopIndex; i++) {
			InsSet inst = prevState.preIssue.buffer.get(i);
			if( (reg1 != null && inst.DST == reg1) || (reg2 != null && inst.DST == reg2)) {
				hazard = true;
				break;
			}
		}
		
		if(!hazard) {
			// check with instructions in the pipeline
			for(InsSet inst: executionBuffer.buffer) {
				if( (reg1 != null && inst.DST == reg1) || (reg2 != null && inst.DST == reg2) ) {
					hazard = true;
					break;
				}
			}
		}
		return hazard;
	}

	private boolean chk_WAW(Integer reg, int stopIndex) {
		boolean hazard = false;
		
		//check with earlier not issued instructions 
		for(int i = 0; i < stopIndex; i++) {
			InsSet inst = prevState.preIssue.buffer.get(i);
			if( reg != null && inst.DST == reg) {
				hazard = true;
				break;
			}
		}
		
		if(!hazard) {
			// check with instructions in the pipeline
			for(InsSet inst: executionBuffer.buffer) {
				if(reg != null && inst.DST == reg) {
					hazard = true;
					break;
				}
			}
		}
		return hazard;
	}	
	
	private boolean chk_WAR(Integer reg, int stopIndex) {
		boolean hazard = false;
		
		//check with earlier not issued instructions 
		for(int i = 0; i < stopIndex; i++) {
			InsSet inst = prevState.preIssue.buffer.get(i);
			if( (inst.SRC1 != null && inst.SRC1 == reg) || (inst.SRC2 != null && inst.SRC2 == reg) ) {
				hazard = true;
				break;
			}
		}
				
		return hazard;	
	}
	
	boolean chk_SWinst(Integer stopIndex) {
		boolean sw_exists = false;
		
		for(int i = 0; i < stopIndex; i++) {
			InsSet inst = prevState.preIssue.buffer.get(i);
			if(inst.opcode == OPCode.SW) {
				sw_exists = true;
				break;
			}
		}
		return sw_exists;
	}

	private boolean procFetch(boolean break_chk_enable) {
		boolean branch_inst = false;
		
		InsSet INS = (InsSet)InsHash.get(PC);
		
		if(INS.instruction.equals("BREAK")) {
			sim_exit = true;
			executedIns = INS;
			return true;
		} 
		
		if (break_chk_enable) {
			// If the next instruction following even a branch inst is a break
			// then we stop the simulation.. do not fetch the branch.. 
			InsSet nextINS = (InsSet)InsHash.get(PC + 4);
			if(nextINS.instruction.equals("BREAK")) {
				sim_exit = true;
				//return false;
			}
		}
		
		if(INS.instType == OPCode.BRANCH && !sim_exit) {
			boolean branchHazard = false;
			branch_inst = true;
			
			if(INS.opcode != OPCode.J) {
				branchHazard = chk_RAW(INS.SRC1, INS.SRC2, prevState.preIssue.count());
			}
			
			if(branchHazard == true) {
				stalled = true;
				waitingIns = INS;
				executedIns = null;
			} else {
				stalled = false;
				executeInstruction(INS);
				waitingIns = null;
				executedIns = INS;
			}
		}				
				
		//if(!(INS.instType == OPCode.BRANCH) && !branch_inst && !(INS.opcode == OPCode.NOP) && !stalled) {
		if(!(INS.opcode == OPCode.BREAK) && !branch_inst && !(INS.opcode == OPCode.NOP) && !stalled) {
			currentState.preIssue.buffer.addLast(INS);
			PC += 4;
		}
		//check if the current instruction is branch, which would not have been executed and
		//the next instruction is BREAK
		else if( (INS.instType == OPCode.BRANCH && sim_exit) || (INS.opcode == OPCode.NOP)) {
			executedIns = INS;
			PC += 4;
		}

	//check if the current instruction is branch, which would not have been executed and
	//the next instruction is BREAK
//	if( (INS.instType == OPCode.BRANCH && sim_exit) || (INS.opcode == OPCode.NOP)) {
//		executedIns = INS;
//		PC += 4;
//	}
//	else if(!(INS.opcode == OPCode.BREAK) && !branch_inst && !(INS.opcode == OPCode.NOP) && !stalled) {
//		currentState.preIssue.buffer.addLast(INS);
//		PC += 4;
//	}	
		
		return branch_inst;
	}
	
	private void iFetch() {
		int empty_slots;
		boolean branch_inst;
		
		//if(sim_exit) return; 
		waitingIns = null;
		executedIns = null;
		branch_inst = false;
		empty_slots = prevState.preIssue.size - prevState.preIssue.count();
		
		//Fetch the 1st instruction	
		if(empty_slots >= 1) {
			//branch_inst = procFetch(empty_slots >= 2);
			branch_inst = procFetch(true);
		}
		
		//Fetch the 2nd instruction
		if(empty_slots >= 2 && !branch_inst) {
			branch_inst = procFetch(false);
		}		
	}
	
	
	private boolean checkHazardsIssue(InsSet inst) {
		// TODO Auto-generated method stub
		int stopIndex = prevState.preIssue.buffer.indexOf(inst);
		boolean rRAW = false;
		boolean rWAW = false;
		boolean rWAR = false;
		boolean rMEM = false;
		boolean issued = false;
		
		switch(inst.instType) {
		case ALU:
			rRAW = chk_RAW(inst.SRC1, inst.SRC2, stopIndex);
			rWAW = chk_WAW(inst.DST, stopIndex);
			rWAR = chk_WAR(inst.DST, stopIndex);
			
			if(prevState.preALU.count() < 2 && currentState.preALU.count() < 2 && !rRAW && !rWAW && !rWAR) {
				prevState.preIssue.buffer.remove(inst);
				currentState.preIssue.buffer.remove(inst);
				currentState.preALU.buffer.addLast(inst);
				issued = true;
			}
			
			break;
		case ALUB:
			rRAW = chk_RAW(inst.SRC1, inst.SRC2, stopIndex);
			rWAW = chk_WAW(inst.DST, stopIndex);
			rWAR = chk_WAR(inst.DST, stopIndex);
			
			if(prevState.preALUB.count() < 2 && currentState.preALUB.count() < 2 && !rRAW && !rWAW && !rWAR) {
				prevState.preIssue.buffer.remove(inst);
				currentState.preIssue.buffer.remove(inst);
				currentState.preALUB.buffer.addLast(inst);
				issued = true;
			}
			break;
		case MEM:
			switch(inst.opcode) {
			case LW:
				//For LW t is W and s is R 
				rMEM = chk_SWinst(stopIndex);
				rRAW = chk_RAW(inst.SRC1, inst.SRC2, stopIndex);
				rWAW = chk_WAW(inst.DST, stopIndex);
				rWAR = chk_WAR(inst.DST, stopIndex);
				
				if(prevState.preMEM.count() < 2 && currentState.preMEM.count() < 2 
				    && !rRAW && !rWAW && !rWAR && !rMEM) {
					prevState.preIssue.buffer.remove(inst);
					currentState.preIssue.buffer.remove(inst);
					currentState.preMEM.buffer.addLast(inst);
					issued = true;
				}
				break;
			case SW:
				//For SW t is R and s is R  
				rMEM = chk_SWinst(stopIndex);
				rRAW = chk_RAW(inst.SRC1, inst.SRC2, stopIndex);
				
				if(prevState.preMEM.count() < 2 && currentState.preMEM.count() < 2 && !rRAW && !rMEM) {
					prevState.preIssue.buffer.remove(inst);
					currentState.preIssue.buffer.remove(inst);
					currentState.preMEM.buffer.addLast(inst);
					issued = true;
				}
				break;
			}
			break;
		}
		
		//If the instruction is issued then push it into the execution buffer as well to keep track of it
		if(issued) {
			executionBuffer.buffer.addLast(inst);
		}
		return issued;
		
	}
	
	private void issue() {
		int issued_count = 0;
		
		//Create a copy of the buffer 
		LinkedList <InsSet> preIssueCopy = new LinkedList<InsSet>(prevState.preIssue.buffer);
		
		for(InsSet inst : preIssueCopy) {
			if(checkHazardsIssue(inst) == true) {
				issued_count++;
			}
			if(issued_count == 2) {
				break;
			}
		}
	}
	
	private void execute() {
		InsSet inst;
		
		//Execute ALU Instructions if any.. 
		if(prevState.preALU.count() >= 1) {
			inst = currentState.preALU.buffer.remove();
			executeInstruction(inst);
			currentState.postALU.buffer.addLast(inst);
		}
		
		//Execute ALUB Instructions if any
		if(prevState.preALUB.count() >= 1) {			
			if(cycleCountPreALUB == 1) {
				inst = currentState.preALUB.buffer.remove();
				executeInstruction(inst);
				currentState.postALUB.buffer.addLast(inst);
				cycleCountPreALUB = 0;
			} else {
				cycleCountPreALUB = 1;
			}
		}
		
		//Execute MEM Instructions if any
		if(prevState.preMEM.count() >= 1) {
			inst = currentState.preMEM.buffer.remove();
			executeInstruction(inst);
			switch(inst.opcode) {
			case LW:
				currentState.postMEM.buffer.addLast(inst);
				break;
			case SW:
				// nothing to do..
				break;
			}
		}
		
	}
	
	void updateRegister(InsSet inst) {
		if(inst.d != null) {
			prevRegState.registers[inst.d] = currentRegState.registers[inst.d];
		}
		if(inst.t != null) {
			prevRegState.registers[inst.t] = currentRegState.registers[inst.t];
		}
	}
	
	private void writeBack() {
		InsSet inst;
		
		// chk postALU Buffer
		if(prevState.postALU.count() >= 1) {
			inst = currentState.postALU.buffer.remove();
			updateRegister(inst);
			executionBuffer.buffer.remove(inst);
		}
		
		// chk postALUB Buffer
		if(prevState.postALUB.count() >= 1) {
			inst = currentState.postALUB.buffer.remove();
			updateRegister(inst);
			executionBuffer.buffer.remove(inst);
		}
		
		//chk postMEM Buffer
		if(prevState.postMEM.count() >= 1) {
			inst = currentState.postMEM.buffer.remove();
			updateRegister(inst);
			executionBuffer.buffer.remove(inst);
		}
	}
	
	private void ProcSimulation(BufferedWriter wbuf) throws IOException {
		
		cycle = 1;
		//int oldPC;
		
		while(!sim_exit) {
			iFetch();
			issue();
			execute();
			writeBack();
			prevState = new PiplineState(currentState);
			printCycle(wbuf, PC);
			cycle++;
		}
		
//		while(inst.equals("BREAK") == false) {
//			InsSet INS = (InsSet)InsHash.get(PC);
//			oldPC = PC;
//			inst = INS.instruction;
//			executeInstruction(INS);
//			printCycle(wbuf, inst, oldPC);
//			cycle++;
//		}

	}
	
	public void execute(String inputFilename, String outputFilename) {
		try {
        	BufferedReader buf = new BufferedReader(new FileReader(inputFilename));
        	
        	BufferedWriter bufw = new BufferedWriter(new FileWriter("disassembly.txt"));
        	ProcAssembly(buf, bufw);
        	bufw.close();
        	
        	BufferedWriter bufsim = new BufferedWriter(new FileWriter(outputFilename));
        	ProcSimulation(bufsim);
    		bufsim.close();
        	buf.close();
        }
        catch(IOException e) {
        	System.out.print("Could not open the input/output files \n");
        }		
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
        //System.out.print(args[0]);
        
        Mipsim Instance = new Mipsim();
        Instance.execute(new String(args[0]), new String(args[1]));   
	}

}
