package com.example.python_and_sharp_mobile; // Убедись, что пакет верный!

import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private MultiAutoCompleteTextView codeInputEditText;
    private TextView outputTextView;
    private Button runCodeButton;
    private Button exampleButton;
    private Spinner languageSpinner;

    // Текущий язык: 0 = Python, 1 = C#
    private int currentLanguage = 0;

    // Ключевые слова Python
    private static final String[] PYTHON_KEYWORDS = {
            "False", "None", "True", "and", "as", "assert", "async", "await", "break",
            "class", "continue", "def", "del", "elif", "else", "except", "finally",
            "for", "from", "global", "if", "import", "in", "is", "lambda", "nonlocal",
            "not", "or", "pass", "raise", "return", "try", "while", "with", "yield",
            "print", "range", "len", "append", "pop", "list", "dict", "set", "str", "int"
    };

    // Ключевые слова C#
    private static final String[] CSHARP_KEYWORDS = {
            "abstract", "as", "base", "bool", "break", "byte", "case", "catch", "char",
            "checked", "class", "const", "continue", "decimal", "default", "delegate",
            "do", "double", "else", "enum", "event", "explicit", "extern", "false",
            "finally", "fixed", "float", "for", "foreach", "goto", "if", "implicit",
            "in", "int", "interface", "internal", "is", "lock", "long", "namespace",
            "new", "null", "object", "operator", "out", "override", "params", "private",
            "protected", "public", "readonly", "ref", "return", "sbyte", "sealed",
            "short", "sizeof", "stackalloc", "static", "string", "struct", "switch",
            "this", "throw", "true", "try", "typeof", "uint", "ulong", "unchecked",
            "unsafe", "ushort", "using", "virtual", "void", "volatile", "while",
            "Console", "WriteLine", "Write", "ReadLine", "var"
    };

    // Класс памяти программы
    private static class Memory {
        HashMap<String, Object> variables = new HashMap<>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        codeInputEditText = (MultiAutoCompleteTextView) findViewById(R.id.codeInputEditText);
        outputTextView = findViewById(R.id.outputTextView);
        runCodeButton = findViewById(R.id.runCodeButton);
        exampleButton = findViewById(R.id.exampleButton);
        languageSpinner = findViewById(R.id.languageSpinner);

        // Настройка Spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.languages, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentLanguage = position;
                updateSyntaxHighlighting();
                updateAutoComplete();
                // Меняем подсказку в поле ввода
                codeInputEditText.setHint(position == 0 ?
                        "# Напиши свой Python-код здесь..." :
                        "// Напиши свой C# код здесь...");
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        setupSyntaxHighlighting();
        updateAutoComplete();

        runCodeButton.setOnClickListener(v -> runSimulatedCompiler());
        exampleButton.setOnClickListener(v -> insertExampleCode());
    }

    private void setupSyntaxHighlighting() {
        codeInputEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable editable) {
                updateSyntaxHighlighting();
                codeInputEditText.setBackgroundColor(Color.parseColor("#252526"));
            }
        });
    }

    private void updateSyntaxHighlighting() {
        Editable editable = codeInputEditText.getText();
        clearSpans(editable);
        String text = editable.toString();
        String[] keywords = (currentLanguage == 0) ? PYTHON_KEYWORDS : CSHARP_KEYWORDS;

        // Ключевые слова
        for (String kw : keywords) {
            Pattern p = Pattern.compile("\\b" + Pattern.quote(kw) + "\\b");
            Matcher m = p.matcher(text);
            while (m.find()) {
                editable.setSpan(new ForegroundColorSpan(0xFF569CD6), m.start(), m.end(), 0);
            }
        }

        // Строки
        Pattern strPattern = Pattern.compile("(\"[^\"\\\\]*(\\\\.[^\"\\\\]*)*\"|'[^'\\\\]*(\\\\.[^'\\\\]*)*')");
        Matcher strMatcher = strPattern.matcher(text);
        while (strMatcher.find()) {
            editable.setSpan(new ForegroundColorSpan(0xFFCE9178), strMatcher.start(), strMatcher.end(), 0);
        }

        // Комментарии
        Pattern commentPattern = (currentLanguage == 0) ?
                Pattern.compile("#[^\n]*") :
                Pattern.compile("//[^\n]*");
        Matcher commentMatcher = commentPattern.matcher(text);
        while (commentMatcher.find()) {
            editable.setSpan(new ForegroundColorSpan(0xFF6A9955), commentMatcher.start(), commentMatcher.end(), 0);
        }

        // Функции / методы
        if (currentLanguage == 0) {
            Pattern funcPattern = Pattern.compile("\\bdef\\s+(\\w+)");
            Matcher funcMatcher = funcPattern.matcher(text);
            while (funcMatcher.find()) {
                editable.setSpan(new ForegroundColorSpan(0xFFDCDCAA), funcMatcher.start(1), funcMatcher.end(1), 0);
            }
        } else {
            // Подсветка методов C# (упрощённо)
            Pattern methodPattern = Pattern.compile("\\b(?:void|int|string|bool|var)\\s+(\\w+)\\s*\\(");
            Matcher methodMatcher = methodPattern.matcher(text);
            while (methodMatcher.find()) {
                editable.setSpan(new ForegroundColorSpan(0xFFDCDCAA), methodMatcher.start(1), methodMatcher.end(1), 0);
            }
        }
    }

    private void clearSpans(Editable editable) {
        for (ForegroundColorSpan span : editable.getSpans(0, editable.length(), ForegroundColorSpan.class)) {
            editable.removeSpan(span);
        }
    }

    private void updateAutoComplete() {
        String[] suggestions = (currentLanguage == 0) ?
                new String[]{"if ", "else:", "elif ", "for ", "while ", "def ", "return ", "print()", "range()", "True", "False", "None"} :
                new String[]{"if ", "else ", "for ", "foreach ", "while ", "return ", "Console.WriteLine()", "var ", "int ", "string ", "bool ", "true", "false", "null"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, suggestions);
        codeInputEditText.setAdapter(adapter);
        codeInputEditText.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        codeInputEditText.setThreshold(1);
    }

    private void insertExampleCode() {
        String example;
        if (currentLanguage == 0) {
            example = "# Пример Python\n" +
                    "x = 10\n" +
                    "if x > 5:\n" +
                    "    print(\"x больше 5\")\n" +
                    "else:\n" +
                    "    print(\"x меньше или равно 5\")\n" +
                    "for i in range(3):\n" +
                    "    print(\"Итерация\", i)";
        } else {
            example = "// Пример C#\n" +
                    "int x = 10;\n" +
                    "if (x > 5) {\n" +
                    "    Console.WriteLine(\"x больше 5\");\n" +
                    "} else {\n" +
                    "    Console.WriteLine(\"x меньше или равно 5\");\n" +
                    "}\n" +
                    "for (int i = 0; i < 3; i++) {\n" +
                    "    Console.WriteLine(\"Итерация \" + i);\n" +
                    "}";
        }
        codeInputEditText.setText(example);
        outputTextView.setText(Html.fromHtml("<font color='#6A9955'>📋 Вставлен пример кода. Нажми 'Выполнить'.</font>"));
    }

    private void runSimulatedCompiler() {
        String code = codeInputEditText.getText().toString().trim();
        StringBuilder output = new StringBuilder();

        if (code.isEmpty()) {
            showError("❌ Ошибка: Файл пуст.");
            return;
        }

        if (currentLanguage == 0) {
            executePython(code, output);
        } else {
            executeCSharp(code, output);
        }

        output.append("<br><font color='#6A9955'>✅ Программа завершена.</font>");
        outputTextView.setText(Html.fromHtml(output.toString(), Html.FROM_HTML_MODE_LEGACY));
    }

    // --------------------- Python ---------------------
    private void executePython(String code, StringBuilder output) {
        Memory mem = new Memory();
        String[] lines = code.split("\n");
        output.append("<font color='#4FC3F7'><b>🐍 Выполнение Python:</b></font><br>");
        executePythonBlock(lines, 0, 0, false, mem, output, 20);
    }

    private int executePythonBlock(String[] lines, int startIdx, int baseIndent,
                                   boolean skipAll, Memory mem, StringBuilder out, int maxLoops) {
        int i = startIdx;
        while (i < lines.length) {
            String line = lines[i];
            String trimmed = line.trim();
            int indent = countLeadingSpaces(line);
            if (trimmed.isEmpty() || trimmed.startsWith("#")) { i++; continue; }
            if (indent < baseIndent) break;
            if (skipAll) { i++; continue; }

            Matcher ifMatcher = Pattern.compile("if\\s+(.+):").matcher(trimmed);
            Matcher elifMatcher = Pattern.compile("elif\\s+(.+):").matcher(trimmed);
            Matcher elseMatcher = Pattern.compile("else:").matcher(trimmed);
            Matcher forMatcher = Pattern.compile("for\\s+(\\w+)\\s+in\\s+(.+):").matcher(trimmed);
            Matcher whileMatcher = Pattern.compile("while\\s+(.+):").matcher(trimmed);

            if (ifMatcher.matches() || elifMatcher.matches()) {
                String cond = ifMatcher.matches() ? ifMatcher.group(1) : elifMatcher.group(1);
                boolean condResult = evaluatePythonCondition(cond, mem);
                out.append("  ❓ Условие ").append(cond).append(" → ").append(condResult).append("<br>");
                int nextIdx = findPythonElseIfEnd(lines, i, indent);
                if (condResult) {
                    i = executePythonBlock(lines, i+1, indent+4, false, mem, out, maxLoops);
                    i = nextIdx;
                } else {
                    i = findPythonNextBranch(lines, i, indent);
                }
                continue;
            }
            if (elseMatcher.matches()) {
                out.append("  ↩ else:<br>");
                int nextIdx = findPythonElseIfEnd(lines, i, indent);
                i = executePythonBlock(lines, i+1, indent+4, false, mem, out, maxLoops);
                i = nextIdx;
                continue;
            }
            if (forMatcher.matches()) {
                String var = forMatcher.group(1);
                String iter = forMatcher.group(2);
                List<Integer> items = resolvePythonIterable(iter, mem);
                if (items == null) { out.append("  ⚠ Неизвестный итератор<br>"); i++; continue; }
                out.append("  🔁 for ").append(var).append(" in ").append(iter).append("<br>");
                int end = findPythonBlockEnd(lines, i, indent);
                for (int val : items) {
                    mem.variables.put(var, val);
                    executePythonBlock(lines, i+1, indent+4, false, mem, out, maxLoops);
                }
                i = end;
                continue;
            }
            if (whileMatcher.matches()) {
                String cond = whileMatcher.group(1);
                out.append("  🔁 while ").append(cond).append("<br>");
                int end = findPythonBlockEnd(lines, i, indent);
                int iter = 0;
                while (iter < maxLoops && evaluatePythonCondition(cond, mem)) {
                    executePythonBlock(lines, i+1, indent+4, false, mem, out, maxLoops);
                    iter++;
                }
                i = end;
                continue;
            }

            // Обычная строка
            executePythonLine(trimmed, mem, out);
            i++;
        }
        return i;
    }

    private void executePythonLine(String line, Memory mem, StringBuilder out) {
        Matcher assign = Pattern.compile("(\\w+)\\s*=\\s*(.+)").matcher(line);
        if (assign.matches() && !line.startsWith("if") && !line.startsWith("for") && !line.startsWith("while")) {
            String var = assign.group(1);
            String expr = assign.group(2);
            try {
                Object val = evaluatePythonExpr(expr, mem);
                mem.variables.put(var, val);
                out.append("  📌 ").append(var).append(" = ").append(formatValue(val)).append("<br>");
            } catch (Exception e) {
                out.append("  ⚠ Ошибка в '").append(expr).append("'<br>");
            }
            return;
        }
        Matcher print = Pattern.compile("print\\((.*)\\)").matcher(line);
        if (print.matches()) {
            String args = print.group(1);
            StringBuilder printed = new StringBuilder();
            for (String arg : splitPythonArgs(args)) {
                try {
                    printed.append(formatValue(evaluatePythonExpr(arg, mem)));
                } catch (Exception e) {
                    printed.append("?");
                }
            }
            out.append("  🖨 ").append(printed).append("<br>");
        }
    }

    private Object evaluatePythonExpr(String expr, Memory mem) throws Exception {
        expr = expr.trim();
        if (expr.startsWith("[") && expr.endsWith("]")) {
            String inner = expr.substring(1, expr.length()-1);
            List<Integer> list = new ArrayList<>();
            if (!inner.isEmpty()) for (String e : inner.split(",")) list.add((Integer)evaluatePythonExpr(e, mem));
            return list;
        }
        Matcher rangeM = Pattern.compile("range\\((\\d+)\\)").matcher(expr);
        if (rangeM.matches()) {
            int n = Integer.parseInt(rangeM.group(1));
            List<Integer> list = new ArrayList<>();
            for (int i=0; i<n; i++) list.add(i);
            return list;
        }
        if (expr.matches("-?\\d+")) return Integer.parseInt(expr);
        if ((expr.startsWith("\"") && expr.endsWith("\"")) || (expr.startsWith("'") && expr.endsWith("'")))
            return expr.substring(1, expr.length()-1);
        if (mem.variables.containsKey(expr)) return mem.variables.get(expr);
        // операции
        if (expr.contains("+")) {
            String[] p = expr.split("\\+", 2);
            Object a = evaluatePythonExpr(p[0], mem);
            Object b = evaluatePythonExpr(p[1], mem);
            if (a instanceof Integer && b instanceof Integer) return (Integer)a + (Integer)b;
            return a.toString() + b.toString();
        }
        throw new Exception("Неизвестное выражение");
    }

    private boolean evaluatePythonCondition(String cond, Memory mem) {
        try {
            Matcher m = Pattern.compile("(.+)\\s*(>|<|==|!=|>=|<=)\\s*(.+)").matcher(cond);
            if (m.matches()) {
                int a = (Integer)evaluatePythonExpr(m.group(1), mem);
                int b = (Integer)evaluatePythonExpr(m.group(3), mem);
                switch (m.group(2)) {
                    case ">": return a > b;
                    case "<": return a < b;
                    case "==": return a == b;
                    case "!=": return a != b;
                }
            }
        } catch (Exception e) {}
        return false;
    }

    private List<Integer> resolvePythonIterable(String expr, Memory mem) {
        try {
            Object obj = evaluatePythonExpr(expr, mem);
            if (obj instanceof List) return (List<Integer>) obj;
        } catch (Exception e) {}
        return null;
    }

    private int findPythonBlockEnd(String[] lines, int start, int baseIndent) {
        for (int i=start+1; i<lines.length; i++) {
            if (lines[i].trim().isEmpty()) continue;
            if (countLeadingSpaces(lines[i]) <= baseIndent) return i;
        }
        return lines.length;
    }

    private int findPythonElseIfEnd(String[] lines, int start, int baseIndent) {
        int i = start+1;
        while (i < lines.length) {
            String t = lines[i].trim();
            int ind = countLeadingSpaces(lines[i]);
            if (t.isEmpty()) { i++; continue; }
            if (ind < baseIndent) break;
            if (ind == baseIndent && !t.startsWith("elif") && !t.startsWith("else")) break;
            i++;
        }
        return i;
    }

    private int findPythonNextBranch(String[] lines, int start, int baseIndent) {
        int i = start+1;
        while (i < lines.length) {
            String t = lines[i].trim();
            int ind = countLeadingSpaces(lines[i]);
            if (t.isEmpty()) { i++; continue; }
            if (ind < baseIndent) break;
            if (ind == baseIndent && (t.startsWith("elif") || t.startsWith("else"))) return i;
            i++;
        }
        return i;
    }

    private List<String> splitPythonArgs(String args) {
        List<String> res = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inStr = false;
        for (char c : args.toCharArray()) {
            if (c == '"' || c == '\'') inStr = !inStr;
            if (!inStr && c == ',') {
                res.add(cur.toString().trim());
                cur = new StringBuilder();
            } else cur.append(c);
        }
        if (cur.length()>0) res.add(cur.toString().trim());
        return res;
    }

    // --------------------- C# ---------------------
    private void executeCSharp(String code, StringBuilder output) {
        output.append("<font color='#4FC3F7'><b>🟦 Выполнение C#:</b></font><br>");
        // Упрощённая симуляция: разбор по строкам с учётом фигурных скобок
        Memory mem = new Memory();
        String[] lines = code.split("\n");
        boolean inMain = false;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("//")) continue;
            if (line.contains("static void Main")) {
                inMain = true;
                output.append("▶ Вход в Main()<br>");
                continue;
            }
            if (!inMain) continue;
            if (line.equals("}")) continue;

            // Присваивание с типом или без
            Matcher assign = Pattern.compile("(?:var|int|string|bool)\\s+(\\w+)\\s*=\\s*(.+);").matcher(line);
            Matcher assign2 = Pattern.compile("(\\w+)\\s*=\\s*(.+);").matcher(line);
            if (assign.matches() || assign2.matches()) {
                Matcher m = assign.matches() ? assign : assign2;
                String var = m.group(1);
                String expr = m.group(2);
                try {
                    Object val = evaluateCSharpExpr(expr, mem);
                    mem.variables.put(var, val);
                    output.append("  📌 ").append(var).append(" = ").append(formatValue(val)).append("<br>");
                } catch (Exception e) {
                    output.append("  ⚠ Ошибка в '").append(expr).append("'<br>");
                }
                continue;
            }

            // Console.WriteLine
            Matcher print = Pattern.compile("Console\\.WriteLine\\(\"(.*)\"(?:\\s*\\+\\s*(.+))?\\);").matcher(line);
            if (print.matches()) {
                String text = print.group(1);
                String expr = print.group(2);
                if (expr != null) {
                    try {
                        Object val = evaluateCSharpExpr(expr, mem);
                        text += formatValue(val);
                    } catch (Exception e) {
                        text += "?";
                    }
                }
                output.append("  🖨 ").append(text).append("<br>");
                continue;
            }

            // if
            Matcher ifM = Pattern.compile("if\\s*\\((.+)\\)\\s*\\{").matcher(line);
            if (ifM.matches()) {
                String cond = ifM.group(1);
                boolean res = evaluateCSharpCondition(cond, mem);
                output.append("  ❓ if (").append(cond).append(") → ").append(res).append("<br>");
                // ищем закрывающую скобку
                int nested = 1;
                int j = i+1;
                while (j < lines.length && nested > 0) {
                    String l = lines[j].trim();
                    if (l.contains("{")) nested++;
                    if (l.contains("}")) nested--;
                    j++;
                }
                if (res) {
                    // выполняем тело (упрощённо)
                }
                i = j-1;
                continue;
            }
        }
    }

    private Object evaluateCSharpExpr(String expr, Memory mem) throws Exception {
        expr = expr.trim().replace(";", "");
        if (expr.matches("-?\\d+")) return Integer.parseInt(expr);
        if (expr.startsWith("\"") && expr.endsWith("\"")) return expr.substring(1, expr.length()-1);
        if (mem.variables.containsKey(expr)) return mem.variables.get(expr);
        if (expr.contains("+")) {
            String[] p = expr.split("\\+", 2);
            Object a = evaluateCSharpExpr(p[0], mem);
            Object b = evaluateCSharpExpr(p[1], mem);
            if (a instanceof Integer && b instanceof Integer) return (Integer)a + (Integer)b;
            return a.toString() + b.toString();
        }
        throw new Exception("?");
    }

    private boolean evaluateCSharpCondition(String cond, Memory mem) {
        try {
            Matcher m = Pattern.compile("(.+)\\s*(>|<|==|!=|>=|<=)\\s*(.+)").matcher(cond);
            if (m.matches()) {
                int a = (Integer)evaluateCSharpExpr(m.group(1), mem);
                int b = (Integer)evaluateCSharpExpr(m.group(3), mem);
                switch (m.group(2)) {
                    case ">": return a > b;
                    case "<": return a < b;
                    case "==": return a == b;
                }
            }
        } catch (Exception e) {}
        return false;
    }

    // --------------------- Общие утилиты ---------------------
    private int countLeadingSpaces(String s) {
        int c = 0;
        while (c < s.length() && s.charAt(c) == ' ') c++;
        return c;
    }

    private String formatValue(Object val) {
        if (val instanceof String) return (String) val;
        return String.valueOf(val);
    }

    private void showError(String msg) {
        outputTextView.setText(Html.fromHtml("<font color='#F44747'>" + msg + "</font>"));
    }

    private void markError() {
        codeInputEditText.setBackgroundColor(Color.parseColor("#3A1F1F"));
    }
}