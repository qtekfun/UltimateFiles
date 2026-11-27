package com.qtekfun.ultimatefiles.domain.usecase

enum class TokenKind { COMMENT, STRING, NUMBER, KEYWORD, TAG }

data class Token(val start: Int, val end: Int, val kind: TokenKind)

/** A small lexical highlighter: comments, strings, numbers, keywords and markup tags. Not a parser. */
object SyntaxHighlighter {
    /** Longer texts are left plain, so typing stays responsive. */
    const val MAX_CHARS = 100_000

    private const val STRING = """"(?:\\.|[^"\\\n])*"|'(?:\\.|[^'\\\n])*'"""
    private const val NUMBER = """\b(?:0[xX][0-9a-fA-F_]+|\d[\d_]*(?:\.\d+)?)[fFLuU]?\b"""
    private const val SLASH_COMMENTS = """//[^\n]*|/\*[\s\S]*?(?:\*/|$)"""
    private const val HASH_COMMENTS = """(?<![^\s])#[^\n]*"""

    private class Language(comments: String?, keywords: String, strings: String = STRING, tags: String? = null) {
        private val words = keywords.split(' ').filter { it.isNotEmpty() }
        val kinds = mutableListOf<TokenKind>()
        val pattern: Regex

        init {
            val groups = mutableListOf<String>()
            fun add(kind: TokenKind, expression: String?) {
                if (expression == null) return
                kinds += kind
                groups += "($expression)"
            }
            add(TokenKind.COMMENT, comments)
            add(TokenKind.STRING, strings)
            add(TokenKind.TAG, tags)
            add(TokenKind.NUMBER, NUMBER)
            add(TokenKind.KEYWORD, words.takeIf { it.isNotEmpty() }?.joinToString("|", """\b(?:""", """)\b"""))
            pattern = Regex(groups.joinToString("|"))
        }
    }

    private val jvm = Language(
        SLASH_COMMENTS,
        "abstract as break by case catch class companion const continue data default do else enum extends false final " +
            "finally for fun if implements import in interface internal is new null object override package private " +
            "protected public return sealed static super suspend switch this throw throws true try val var void when while " +
            "int long boolean double float char byte short",
    )
    private val c = Language(
        SLASH_COMMENTS,
        "auto bool break case char const continue default do double else enum extern false float fn for func go if impl " +
            "int let long match mod mut namespace nil null pub return short signed sizeof static struct switch template " +
            "true type typedef union unsigned use var void volatile while",
    )
    private val script = Language(
        SLASH_COMMENTS,
        "async await break case catch class const continue default delete do else export extends false finally for from " +
            "function if import in instanceof interface let new null of return switch this throw true try type typeof " +
            "undefined var while yield",
        strings = STRING + """|`(?:\\.|[^`\\])*`""",
    )
    private val python = Language(
        HASH_COMMENTS,
        "and as assert async await break class continue def del elif else except False finally for from global if import " +
            "in is lambda None nonlocal not or pass raise return True try while with yield",
    )
    private val shell = Language(
        HASH_COMMENTS,
        "case do done elif else esac exit export fi for function if in local read return set then unset until while",
    )
    private val config = Language("$HASH_COMMENTS|(?<![^\\s]);[^\\n]*", "true false null yes no on off")
    private val json = Language(null, "true false null")
    private val sql = Language(
        """--[^\n]*|/\*[\s\S]*?(?:\*/|$)""",
        "SELECT FROM WHERE INSERT INTO VALUES UPDATE SET DELETE CREATE TABLE DROP ALTER JOIN LEFT INNER ON GROUP BY ORDER " +
            "LIMIT AND OR NOT NULL PRIMARY KEY select from where insert into values update set delete create table drop " +
            "alter join left inner on group by order limit and or not null primary key",
    )
    private val markup = Language("""<!--[\s\S]*?(?:-->|$)""", "", tags = """</?[A-Za-z][\w:.-]*|/?>""")
    private val styles = Language("""/\*[\s\S]*?(?:\*/|$)""", "important")

    private fun languageOf(name: String): Language? = when (name.substringAfterLast('.', "").lowercase()) {
        "kt", "kts", "java", "gradle" -> jvm
        "c", "h", "cpp", "rs", "go" -> c
        "js", "ts" -> script
        "py" -> python
        "sh" -> shell
        "yml", "yaml", "toml", "ini", "conf", "cfg", "properties" -> config
        "json" -> json
        "sql" -> sql
        "xml", "html", "htm", "svg" -> markup
        "css" -> styles
        else -> null
    }

    fun tokens(name: String, text: CharSequence): List<Token> {
        val language = languageOf(name) ?: return emptyList()
        if (text.length > MAX_CHARS) return emptyList()
        return language.pattern.findAll(text).map { match ->
            val group = language.kinds.indices.first { match.groups[it + 1] != null }
            Token(match.range.first, match.range.last + 1, language.kinds[group])
        }.toList()
    }
}
