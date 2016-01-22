package scanner;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import exceptions.IllegalBlockCommentException;
import exceptions.IllegalCharException;
import exceptions.IllegalIDException;
import exceptions.IllegalInputCharException;


public class Scanner {
    private Reader _in;
    private StringBuilder _sb;  // builds lexemes
    private int _next;          // character read
    private List<Token> _tokens;
    
    private final Map<Character, RunnableScan> opMap;
    private final Map<Character, TokenType> sepMap;
    private final Map<String, TokenType> idMap;
    private final Set<Character> ESCAPES;
    
    private interface RunnableScan {
        void run() throws IOException, IllegalInputCharException, IllegalBlockCommentException;
    }

    public Scanner(Reader in) {

        _in = in;
        _sb = new StringBuilder();
        _next = -1;
        _tokens = null;
        
        // Example for organizing functions for operators
        opMap = new HashMap<Character, RunnableScan>();
        initOpMap();
        
        sepMap = new HashMap<Character, TokenType>();        
        initSepMap();
	
        idMap = new HashMap<String, TokenType>();
        initIdMap();
        
        ESCAPES = new TreeSet<Character>();
        for (char e : "btnfr\"\'\\".toCharArray()) {
        	ESCAPES.add(e);
        }
    }

    private void initSepMap() {
    	sepMap.put('(', TokenType.LPAREN);
		sepMap.put(')', TokenType.RPAREN);
		sepMap.put('{', TokenType.LBRACE);
		sepMap.put('}', TokenType.RBRACE);
		sepMap.put('[', TokenType.LBRACKET);
		sepMap.put(']', TokenType.RBRACKET);
		sepMap.put(';', TokenType.SEMICOLON);
		sepMap.put(',', TokenType.COMMA);
		sepMap.put('.', TokenType.DOT);		   
    }
    
    private void initOpMap() {
        opMap.put('>', scanRangle);
        opMap.put('=', scanAssign);
        opMap.put('<', scanLangle);
        opMap.put('!', scanExclamation);
        opMap.put('?', scanQuestion);
        opMap.put(':', scanColon);
        opMap.put('&', scanAmpersand);
        opMap.put('|', scanVertical);
        opMap.put('^', scanCaret);
        opMap.put('+', scanPlus);
        opMap.put('-', scanMinus);
        opMap.put('*', scanStar);
        opMap.put('/', scanSlash);
        opMap.put('%', scanPercent);
        opMap.put('~', scanTilde);
    }

    private void initIdMap() {
    	// technically literals
    	idMap.put("true", TokenType.TRUE);
    	idMap.put("false", TokenType.FALSE);
        idMap.put("null", TokenType.NULL);
        // keywords
        idMap.put("abstract", TokenType.ABSTRACT);
        idMap.put("boolean", TokenType.BOOLEAN);
        idMap.put("break", TokenType.BREAK);
        idMap.put("byte", TokenType.BYTE);
        idMap.put("case", TokenType.CASE);
        idMap.put("catch", TokenType.CATCH);
        idMap.put("char", TokenType.CHAR);
        idMap.put("class", TokenType.CLASS);
        idMap.put("const", TokenType.CONST);
        idMap.put("continue", TokenType.CONTINUE);
        idMap.put("default", TokenType.DEFAULT);
        idMap.put("do", TokenType.DO);
        idMap.put("double", TokenType.DOUBLE);
        idMap.put("else", TokenType.ELSE);
        idMap.put("extends", TokenType.EXTENDS);
        idMap.put("final", TokenType.FINAL);
        idMap.put("finally", TokenType.FINALLY);
        idMap.put("float", TokenType.FLOAT);
        idMap.put("for", TokenType.FOR);
        idMap.put("goto", TokenType.GOTO);
        idMap.put("if", TokenType.IF);
        idMap.put("implements", TokenType.IMPLEMENTS);
        idMap.put("import", TokenType.IMPORT);
        idMap.put("instanceof", TokenType.INSTANCEOF);
        idMap.put("int", TokenType.INT);
        idMap.put("interface", TokenType.INTERFACE);
        idMap.put("long", TokenType.LONG);
        idMap.put("native", TokenType.NATIVE);
        idMap.put("new", TokenType.NEW);
        idMap.put("package", TokenType.PACKAGE);
        idMap.put("private", TokenType.PRIVATE);
        idMap.put("protected", TokenType.PROTECTED);
        idMap.put("public", TokenType.PUBLIC);
        idMap.put("return", TokenType.RETURN);
        idMap.put("short", TokenType.SHORT);
        idMap.put("static", TokenType.STATIC);
        idMap.put("strictfp", TokenType.STRICTFP);
        idMap.put("super", TokenType.SUPER);
        idMap.put("switch", TokenType.SWITCH);
        idMap.put("synchronized", TokenType.SYNCHRONIZED);
        idMap.put("this", TokenType.THIS);
        idMap.put("throw", TokenType.THROW);
        idMap.put("throws", TokenType.THROWS);
        idMap.put("transient", TokenType.TRANSIENT);
        idMap.put("try", TokenType.TRY);
        idMap.put("void", TokenType.VOID);
        idMap.put("volatile", TokenType.VOLATILE);
        idMap.put("while", TokenType.WHILE);
    }

    private int read() throws IOException, IllegalInputCharException {
        int c = _in.read();
        if (c < -1 || c > 127) {
            throw new IllegalInputCharException();
        }
        return c;
    }

    public List<Token> scan() {
        // if scan has already been called, just return the same list
        if (_tokens == null) {
            _tokens = new ArrayList<Token>();
            try {
                // need to use the return value. --Z
                scanStart();
            } catch (IOException ioe) {
                // handle IOException
            } catch (IllegalIDException ide) {
            	ide.printStackTrace();
            } catch (IllegalCharException ice) {
            	ice.printStackTrace();
            } catch (IllegalBlockCommentException ibce) {
                ibce.printStackTrace();
            } catch (IllegalInputCharException iice) {
                iice.printStackTrace();
            } catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }

        return _tokens;
    }
    
    /**
     * For Testing. --Z
     * @return
     * @throws Exception
     */
    public List<Token> scanThrow() throws Exception {
    	_tokens = new ArrayList<Token>();
    	scanStart();
    	return _tokens;
    }

    private void scanStart() throws Exception {
        _next = read();
        for ( ; ; ) {
            /* The loop intentionally does not call read() here--not all tokens are terminated with whitespace;
             * e.g., "scanStart();" needs to read the '(' to find the end of the ID and return.
             * If read() was called in this loop, the LPAREN will be skipped.
             */
            while (Character.isWhitespace(_next)) {
                _next = read();
            }

            if (_next == -1) { //end of file
                break;
            }

            _sb.setLength(0); //clear StringBuilder
            
            if (Character.isLetter(_next)) {
                scanId();
            } else if (Character.isDigit(_next)) {
            	// integer literals
            	scanInteger();
            } else if ('\'' == (char) _next) {
            	// character literals
            	scanChar();
            } else if ('\"' == (char) _next) {
            	scanString();
            } else if (sepMap.containsKey((char) _next)) {
            	//find TokenType.
            	scanSeparators();
            } else if (opMap.containsKey((char) _next)) {
            	opMap.get((char) _next).run();
            } else {
                throw new RuntimeException("input " + (char) _next + "["
                        + (char) _next + "] not yet implemented");
            }
        }
    }

    private void scanId() throws IOException, IllegalInputCharException {
        for ( ; ; ) {
            _sb.append((char) _next);
            _next = read();
            if (!Character.isLetterOrDigit(_next) && _next != '_' && _next != '$') {
                String lexeme = _sb.toString();
                TokenType type = (idMap.containsKey(lexeme) ? idMap.get(lexeme) : TokenType.ID);
                _tokens.add(new Token(lexeme, type));
                return;
            }
        }
    }

    private RunnableScan scanRangle = new RunnableScan() {
        public void run() throws IOException, IllegalInputCharException{
            TokenType tokenType = TokenType.RANGLE;
            _sb.append((char) _next);

            for (;;) {
                _next = read();
                if (_next == '>' && tokenType.equals(TokenType.RANGLE)) {
                    tokenType = TokenType.DBRANGLE;
                } else if (_next == '>' && tokenType.equals(TokenType.DBRANGLE)) {
                    tokenType = TokenType.TPRANGLE;
                } else if (_next == '=' && tokenType.equals(TokenType.RANGLE)) {
                    tokenType = TokenType.GEQ;
                } else if (_next == '=' && tokenType.equals(TokenType.DBRANGLE)) {
                    tokenType = TokenType.RSHIFT_EQ;
                } else if (_next == '=' && tokenType.equals(TokenType.TPRANGLE)) {
                    tokenType = TokenType.URSHIFT_EQ;
                } else {
                    break;
                }
                _sb.append((char) _next);
            }
            _tokens.add(new Token(_sb.toString(), tokenType));
        }
    };

    private RunnableScan scanLangle = new RunnableScan() {

        public void run() throws IOException, IllegalInputCharException{
            TokenType tokenType = TokenType.LANGLE;
            _sb.append((char) _next);

            for (;;) {
                _next = read();
                if (_next == '<' && tokenType.equals(TokenType.LANGLE)) {
                    tokenType = TokenType.DBLANGLE;
                } else if (_next == '=' && tokenType.equals(TokenType.LANGLE)) {
                    tokenType = TokenType.LEQ;
                } else if (_next == '=' && tokenType.equals(TokenType.DBLANGLE)) {
                    tokenType = TokenType.LSHIFT_EQ;
                } else {
                    break;
                }
                _sb.append((char) _next);
            }
            _tokens.add(new Token(_sb.toString(), tokenType));
        }
    };

    private void scanTwoOptionsOp(TokenType defaultType, char secondChar,
            TokenType twoCharsType) throws IOException, IllegalInputCharException {
        TokenType tokenType = defaultType;
        _sb.append((char) _next);
        for (;;) {
            _next = read();
            if (_next == secondChar && tokenType.equals(defaultType)) {
                tokenType = twoCharsType;
            } else {
                break;
            }
            _sb.append((char) _next);
        }
        _tokens.add(new Token(_sb.toString(), tokenType));
    }

    private void scanThreeOptionsOp(TokenType defaultType, char firstOption,
            TokenType firstType, char secondOption, TokenType secondType)
            throws IOException, IllegalInputCharException {
        TokenType tokenType = defaultType;
        _sb.append((char) _next);
        for (;;) {
            _next = read();
            if (_next == firstOption && tokenType.equals(defaultType)) {
                tokenType = firstType;
            } else if (_next == secondOption && tokenType.equals(defaultType)) {
                tokenType = secondType;
            } else {
                break;
            }
        _sb.append((char) _next);
        }
        _tokens.add(new Token(_sb.toString(), tokenType));
    }

    private RunnableScan scanAssign = new RunnableScan() {

        public void run() throws IOException, IllegalInputCharException {
            scanTwoOptionsOp(TokenType.ASSIGN, '=', TokenType.EQUAL);
        }
    };

    private RunnableScan scanExclamation = new RunnableScan() {

        public void run() throws IOException, IllegalInputCharException {
            scanTwoOptionsOp(TokenType.NOT, '=', TokenType.NEQ);
        }
    };

    private RunnableScan scanQuestion = new RunnableScan() {

        public void run() throws IOException, IllegalInputCharException {
            _sb.append((char) _next);
            _tokens.add(new Token(_sb.toString(), TokenType.QUESTION));
            _next = read();
        }
    };

    private RunnableScan scanTilde = new RunnableScan() {

        public void run() throws IOException, IllegalInputCharException {
            _sb.append((char) _next);
            _tokens.add(new Token(_sb.toString(), TokenType.BIT_COMP));
            _next = read();
        }
    };

    private RunnableScan scanColon = new RunnableScan() {

        public void run() throws IOException, IllegalInputCharException {
            _sb.append((char) _next);
            _tokens.add(new Token(_sb.toString(), TokenType.COLON));
            _next = read();
        }
    };

    private RunnableScan scanAmpersand = new RunnableScan() {

        public void run() throws IOException, IllegalInputCharException {
            scanThreeOptionsOp(TokenType.BITAND, '&', TokenType.AND, '=',
                    TokenType.AND_EQ);
        }
    };

    private RunnableScan scanVertical = new RunnableScan() {

        public void run() throws IOException, IllegalInputCharException {
            scanThreeOptionsOp(TokenType.BITOR, '|', TokenType.LOR, '=',
                    TokenType.OR_EQ);
        }
    };

    private RunnableScan scanCaret = new RunnableScan() {

        public void run() throws IOException, IllegalInputCharException {
            scanTwoOptionsOp(TokenType.EXOR, '=', TokenType.EXOR_EQ);
        }
    };

    private RunnableScan scanPlus = new RunnableScan() {

        public void run() throws IOException, IllegalInputCharException {
            scanThreeOptionsOp(TokenType.PLUS, '+', TokenType.INCREMENT, '=',
                    TokenType.PLUS_EQ);
        }
    };

    private RunnableScan scanMinus = new RunnableScan() {

        public void run() throws IOException, IllegalInputCharException {
            scanThreeOptionsOp(TokenType.MINUS, '-', TokenType.DECREMENT, '=',
                    TokenType.MINUS_EQ);
        }
    };

    private RunnableScan scanStar = new RunnableScan() {

        public void run() throws IOException, IllegalInputCharException {
            scanTwoOptionsOp(TokenType.STAR, '=', TokenType.STAR_EQ);
        }
    };

    private RunnableScan scanSlash = new RunnableScan() {
        public void run() throws IOException, IllegalInputCharException, IllegalBlockCommentException {
            _next = read();
            if (_next == '/') {         // in-line comment
                do {
                    _next = read();
                } while (_next != '\n' && _next != '\r' && _next != -1);

                // Let scanStart deal with the character(s)
            } else if (_next == '*') {  // block comment
                _next = read();
                for ( ; ; ) {
                    if (_next == -1) {
                        throw new IllegalBlockCommentException();
                    }
                    if (_next == '*') {
                        _next = read();
                        if (_next == '/') {
                            break;
                        }
                    } else { // necessary since "**/" can end a comment
                        _next = read();
                    }
                }

                _next = read();
            } else if (_next == '=') {
                _tokens.add(new Token("/=", TokenType.SLASH_EQ));

                _next = read();
            } else {
                _tokens.add(new Token("/", TokenType.SLASH));
            }
        }
    };

    private RunnableScan scanPercent = new RunnableScan() {

        public void run() throws IOException, IllegalInputCharException {
            scanTwoOptionsOp(TokenType.MOD, '=', TokenType.MOD_EQ);
        }
    };
    
    /**
     * scanning separators
     * @throws IOException
     */
    private void scanSeparators() throws IOException, IllegalInputCharException {
    	_sb.append((char) _next);
        String lexeme = _sb.toString();
        _tokens.add(new Token(lexeme, sepMap.get(lexeme.charAt(0))));
    	_next = read();
    }
    
    /**
     * scanning integer literals
     * NOTE: 
     * 		1. leaving size check for parser.
     * 		2. No Octal, Hex or Long.
     * 		3. Keeping the string as "\'c\'" for now.
     * @throws IOException
     * @throws IllegalIDException 
     */
    private void scanInteger() throws IOException, IllegalInputCharException, IllegalIDException {
    	while (Character.isDigit(_next)) {
    		_sb.append((char) _next);
    		_next = read();
    	}
    	_tokens.add(new Token(_sb.toString(), TokenType.DECIMAL));
    	// A proper integer must be terminated with space, operators or ';'.
    	// check for illegal identifiers.
    	if (Character.isLetter(_next) || _next == '_' || _next == '$') {
    		throw new IllegalIDException(_sb.toString() + (char) _next);
    	}
    }
    
    /**
     * scanning character literals
     * @throws IOException
     * @throws IllegalCharException
     */
    private void scanChar() throws IOException, IllegalInputCharException, IllegalCharException {
    	_sb.append((char) _next);
    	_next = read();
    	
    	// single character
    	readChar();
    	
    	// terminating literal
    	_next = read();
    	if ('\'' != _next) {
    		throw new IllegalCharException(_sb.toString() + (char) _next + '\'');
    	}
    	_sb.append((char) _next);
    	_tokens.add(new Token(_sb.toString(), TokenType.CHARACTER));
    	
    	_next = read();
    }
    
    /**
     * scanning string literals
     * @throws IOException
     * @throws IllegalCharException
     */
    private void scanString() throws IOException, IllegalInputCharException, IllegalCharException  {
    	_sb.append((char) _next);
    	_next = read();
    	while ('\"' != _next) {
    		readChar();
    		_next = read();
    	}
    	
    	_sb.append((char) _next);
    	_tokens.add(new Token(_sb.toString(), TokenType.STRING));
    	
    	_next = read();
    }
    
    /**
     * Helper for reading a single character and putting it in _sb.
     * Used by scanChar() and scanString(). Not to be confused with scanChar().
     * @throws IOException
     * @throws IllegalCharException
     */
    private void readChar() throws IOException, IllegalInputCharException, IllegalCharException {
    	// single character
    	if ('\\' == _next) {
    		// escape character
    		readEscape();
    	} else {
    		// normal character
    		_sb.append((char) _next);
    	}
    }
    
    /**
     * reading escape character.
     * Octal and Unicode Escape not implemented.
     * @throws IOException
     * @throws IllegalCharException
     */
    private void readEscape() throws IOException, IllegalInputCharException, IllegalCharException {
    	_sb.append((char) _next);
    	_next = read();
    	if (!ESCAPES.contains((char) _next)) {
    		throw new IllegalCharException(_sb.toString() + (char) _next + '\'');
    	}
    	_sb.append((char) _next);
    }
    
}
