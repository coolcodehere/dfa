import java.util.*;
import java.io.*;

class DFA {
  public static void main(String[] args) {
		DFAClassParser parser = new DFAClassParser();
		DFAClass minDFAClass = parser.parseFile(args[0]).hopcroftMinimization();
		minDFAClass.print();
		minDFAClass.printInputResults(args[1]);
  }
}

class DFAClassParser {
	public DFAClass parseFile(String filename) {
		return buildDFAClass(getLines(filename));
	}

	public DFAClass buildDFAClass(ArrayList<String> inputLines) {
		DFAClass DFAClass = new DFAClass();

		String[] validInputs = inputLines.get(1).split(":")[1].trim().replaceAll(" +", " ").split(" ");

		for (String input : validInputs) {
			DFAClass.addInput(input.charAt(0));
		}

		String initialState = inputLines.get(inputLines.size() - 2).split(":")[0];
		DFAClass.initial = Integer.parseInt(initialState);
		String[] acceptingStates = inputLines.get(inputLines.size() - 1).split(":")[0].split(",");
		
		for (String state : acceptingStates) {
			DFAClass.addState(Integer.parseInt(state), true);
		}

		inputLines.remove(inputLines.size() - 1);
		inputLines.remove(inputLines.size() - 1);
		inputLines.remove(inputLines.size() - 1);
		inputLines.remove(0);
		inputLines.remove(0);
		inputLines.remove(0);

		for (String line : inputLines) {
			line = line.trim().replaceAll(" +", " ").replace(":","");
			String[] stateMapping = line.split(" ");
			DFAClass.addState(Integer.parseInt(stateMapping[0]), false);

			for (int i = 1; i < DFAClass.validInputs.size() + 1; i++) {
				DFAClass.addTransition(Integer.parseInt(stateMapping[0]), 
				validInputs[i-1].charAt(0), 
				Integer.parseInt(stateMapping[i]));
			}
		}
	
		return DFAClass;
	}

	public ArrayList<String> getLines(String filename) {
		ArrayList<String> input = new ArrayList<>();
		Scanner scan = null;
		try {
			scan = new Scanner(new File(filename));
		} catch (Exception e) {

		}
		
		while(scan.hasNextLine()) {
			input.add(scan.nextLine());
		}
		
		return input;
	}
}

class TransitionMap {
	public HashMap<Character, Set<Integer>> inputToState = new HashMap<>();
	public HashMap<Integer, HashMap<Character, Integer>> map = new HashMap<>();

	boolean doesInputLeadToState(char input, int state) {
		return inputToState.get(input).contains(state);
	}

	public void addTransition(int fromState, char input, int toState) {
		if (!map.containsKey(fromState)) {
			map.put(fromState, new HashMap<Character, Integer>());
		}
		map.get(fromState).put(input, toState);
		

		if (!inputToState.containsKey(input)) {
			inputToState.put(input, new HashSet<Integer>());
		}
		inputToState.get(input).add(toState);
	}

	public void printTransitions() {
		Object[] keys = map.keySet().toArray();

		for (Object key : keys) {
			System.out.println(key + ": " + map.get(key).toString());
		}
	}
}

class DFAClass {
	Set<Integer> accepting = new HashSet<>();
	Set<Integer> states = new HashSet<>();
	int initial;
	Set<Character> validInputs = new HashSet<>();
	TransitionMap map = new TransitionMap();
	
	public void addState(int state, boolean isAccepting) {
		if (isAccepting) {
			accepting.add(state);
		}

		states.add(state);
	}

	public boolean contains(int state) {
		return states.contains(state);
	}

	public void addInput(char input) {
		validInputs.add(input);
	}

	public void addTransition(int fromState, char input, int toState) {
		map.addTransition(fromState, input, toState);
	}

	public int transition(int fromState, char input) {
		return map.map.get(fromState).get(input);
	}

	public DFAClass minStatesToDFAClass(Set<Set<Integer>> P) {
		DFAClass DFAClass = new DFAClass();
		DFAClass.validInputs.addAll(validInputs);

		Object[] pArr = P.toArray();
		for (int i = 0; i < pArr.length; i++) {
			if(!Collections.disjoint(accepting, (Set<Integer>)pArr[i])) {
				DFAClass.addState(i, true);
			} else {
				DFAClass.addState(i, false);
			}
		}

		for (int i = 0; i < pArr.length; i++) {
			for (int oldState : (Set<Integer>)pArr[i]) {
				for (char input : validInputs) {
					int toOldState = map.map.get(oldState).get(input);
					for (int j = 0; j < pArr.length; j++) {
						if (((Set<Integer>)pArr[j]).contains(toOldState)) {
							DFAClass.addTransition(i, input, j);
						}
					}
				}
			}
		}

		return DFAClass;
	}

	public DFAClass hopcroftMinimization() {
		Set<Set<Integer>> P = new HashSet<>();
		Set<Set<Integer>> W = new HashSet<>();
		P.add(new HashSet<Integer>(accepting));
		P.add(setSubtraction(states, accepting));
		W.addAll(P);

		while (!W.isEmpty()) {
			Object[] wArr = W.toArray();
			Object[] pArr = P.toArray();

			for (Object A : wArr) {
				W.remove(A);
				for (char input : validInputs) {
					Set<Integer> X = findX((Set)A, input);

					for (Object Y : pArr) {
						Set<Integer> xAndY = intersection(X, (Set)Y);
						Set<Integer> yNotX = setSubtraction((Set)Y, X);
						if (!xAndY.isEmpty() && !yNotX.isEmpty()) {
							P.remove(Y);
							P.add(xAndY);
							P.add(yNotX);
							if (W.contains(Y)) {
								W.remove(Y);
								W.add(xAndY);
								W.add(yNotX);
							} else {
								if (xAndY.size() <= yNotX.size()) {
									W.add(xAndY);
								} else {
									W.add(yNotX);
								}
							}
						} 
					}
				}
				W.remove(new HashSet<>());
			}
		}

		return minStatesToDFAClass(P);
	}

	public void print() {
		System.out.println("Sigma: ");
		ArrayList<Character> sigma = new ArrayList<>(validInputs);
		Collections.sort(sigma);
		ArrayList<Integer> stateLabels = new ArrayList<>(states);
		Collections.sort(stateLabels);
		
		System.out.printf("%3s", "");
		for (int i = 0; i < sigma.size(); i++) {
			System.out.printf("%20c", sigma.get(i));
		}
		
		System.out.println("\n---------------------------------------------------------------------------------------------");
		
		for (int i = 0; i < stateLabels.size(); i++) {
			System.out.printf("%3d:", stateLabels.get(i));
			int currState = stateLabels.get(i);
			for (int j = 0; j < sigma.size(); j++) {
				int nextState = transition(currState, sigma.get(j));
				System.out.printf("%20d", nextState);	
			}
			System.out.println();
		}
		System.out.println("---------------------------------------------------------------------------------------------");
		System.out.printf("Initial State: %d\n", initial);
		System.out.printf("Accepting State(s): %s\n", accepting.toString());
	}
	
	public void printInputResults(String inputFilename) {
		Scanner scan = null;
		try {
			scan = new Scanner(new File(inputFilename));
		} catch(Exception e) {

		}
		System.out.println("Parsing results of strings in strings.txt:");

		while (scan.hasNextLine()) {
			String input = scan.nextLine();
			int currState = initial;
			boolean good = isGoodInput(input);
			if (good) {
				for (int i = 0; i < input.length(); i++) {
						currState = transition(currState, input.charAt(i));
				}
			} 
			
			if (accepting.contains(currState) && good) {
				System.out.print("Yes ");
			} else {
				System.out.print("No ");
			}
			
		}
	}

	public boolean isGoodInput(String input) {
		for (int i = 0; i < input.length(); i++) {
			if (!validInputs.contains(input.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	public Set<Integer> intersection(Set<Integer> A, Set<Integer> B) {
		Set<Integer> ret = new HashSet<>();
		ret.addAll(A);
		ret.retainAll(B);
		return ret;
	}

	public Set<Integer> setSubtraction(Set<Integer> A, Set<Integer> B) {
		Set<Integer> ret = new HashSet<>();
		ret.addAll(A);
		ret.removeAll(B);
		return ret;
	}

	public Set<Integer> findX(Set<Integer> currA, char currC) {
		Set<Integer> X = new HashSet<>();
		for (int state : currA) {
			if (map.doesInputLeadToState(currC, state)) {
				X.add(state);
			}
		}
		return X;
	}
}
