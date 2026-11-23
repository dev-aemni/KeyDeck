package com.example.keyboard

import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

data class SuggestionSlot(
    val text: String,
    val isLiteralTyped: Boolean = false,
    val isAutoCorrectHighlight: Boolean = false,
    val commitTextValue: String = text,
)

data class SuggestionEvalResult(
    val slots: List<SuggestionSlot>,
    val autoCorrectReplacement: String?,
)

data class PendingAutoCorrectUndo(
    val originalWord: String,
    val replacedTextWithSeparator: String,
)

data class AutoCorrectAction(
    val originalWord: String,
    val replacementWithSeparator: String,
)

/**
 * Lightweight, 100% offline English frequency dictionary + prefix/fuzzy engine
 * with QWERTY key proximity weighting and strict one-time autocorrect rules.
 */
object SimpleSuggestionEngine {

    private val commonWordsOrdered: List<String> by lazy {
        COMMON_WORDS_RAW.split(' ', '\n')
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .toList()
    }

    private val wordRankMap: Map<String, Int> by lazy {
        buildMap(commonWordsOrdered.size) {
            commonWordsOrdered.forEachIndexed { idx, w ->
                put(w.lowercase(Locale.US), idx)
            }
        }
    }

    private val commonTypos: Map<String, String> = mapOf(
        "teh" to "the",
        "hte" to "the",
        "adn" to "and",
        "nad" to "and",
        "taht" to "that",
        "tath" to "that",
        "wiht" to "with",
        "wtih" to "with",
        "waht" to "what",
        "whta" to "what",
        "jsut" to "just",
        "jstu" to "just",
        "liek" to "like",
        "lkie" to "like",
        "ahve" to "have",
        "hvae" to "have",
        "yuo" to "you",
        "yuor" to "your",
        "yoru" to "your",
        "tihs" to "this",
        "htis" to "this",
        "becuase" to "because",
        "beacuse" to "because",
        "becasue" to "because",
        "thier" to "their",
        "freind" to "friend",
        "beleive" to "believe",
        "recieve" to "receive",
        "acheive" to "achieve",
        "seperate" to "separate",
        "definately" to "definitely",
        "definatly" to "definitely",
        "occured" to "occurred",
        "untill" to "until",
        "wich" to "which",
        "whcih" to "which",
        "helo" to "hello",
        "hllo" to "hello",
        "elloh" to "hello",
        "wrld" to "world",
        "wrold" to "world",
        "keybaord" to "keyboard",
        "keybord" to "keyboard",
        "mesage" to "message",
        "messaage" to "message",
        "tommorrow" to "tomorrow",
        "tommorow" to "tomorrow",
        "neccessary" to "necessary",
        "goverment" to "government",
        "enviroment" to "environment",
        "langauge" to "language",
        "im" to "I'm",
        "ive" to "I've",
        "ill" to "I'll",
        "dont" to "don't",
        "cant" to "can't",
        "wont" to "won't",
        "didnt" to "didn't",
        "isnt" to "isn't",
        "arent" to "aren't",
        "doesnt" to "doesn't",
        "havent" to "haven't",
        "hasnt" to "hasn't",
        "hadnt" to "hadn't",
        "wasnt" to "wasn't",
        "werent" to "weren't",
        "wouldnt" to "wouldn't",
        "couldnt" to "couldn't",
        "shouldnt" to "shouldn't",
        "youre" to "you're",
        "theyre" to "they're",
        "thats" to "that's",
        "whats" to "what's",
        "heres" to "here's",
        "theres" to "there's",
        "lets" to "let's",
    )

    private val qwertyCoords: Map<Char, Pair<Float, Float>> = buildMap {
        val rows = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
        val xOffsets = listOf(0f, 0.35f, 0.85f)
        rows.forEachIndexed { rIdx, row ->
            row.forEachIndexed { cIdx, ch ->
                put(ch, (cIdx + xOffsets[rIdx]) to rIdx.toFloat())
            }
        }
    }

    private val defaultStarters = listOf("I", "The", "Hello")
    private val midSentenceFallbacks = listOf("and", "to", "the")

    private val trigramPredictions: Map<String, List<String>> = mapOf(
        "i love" to listOf("you", "it", "that"),
        "love you" to listOf("so", "too", "more"),
        "i like" to listOf("it", "this", "that"),
        "i want" to listOf("to", "you", "this"),
        "i need" to listOf("to", "help", "this"),
        "i am" to listOf("so", "good", "sorry"),
        "i'm so" to listOf("sorry", "happy", "glad"),
        "i have" to listOf("a", "to", "been"),
        "i think" to listOf("so", "it", "that"),
        "i know" to listOf("you", "it", "that"),
        "i miss" to listOf("you", "it", "this"),
        "i hope" to listOf("you", "so", "it"),
        "i feel" to listOf("like", "so", "good"),
        "i don't" to listOf("know", "think", "want"),
        "i can't" to listOf("wait", "believe", "do"),
        "i will" to listOf("be", "do", "send"),
        "thank you" to listOf("so", "very", "much"),
        "thanks for" to listOf("the", "your", "everything"),
        "how are" to listOf("you", "things", "we"),
        "what are" to listOf("you", "we", "they"),
        "where are" to listOf("you", "we", "they"),
        "what is" to listOf("the", "this", "that"),
        "who is" to listOf("that", "this", "there"),
        "how is" to listOf("it", "the", "your"),
        "how much" to listOf("is", "does", "time"),
        "going to" to listOf("be", "do", "have"),
        "want to" to listOf("go", "see", "do"),
        "need to" to listOf("be", "do", "get"),
        "have to" to listOf("go", "be", "do"),
        "try to" to listOf("make", "get", "find"),
        "used to" to listOf("be", "have", "do"),
        "nice to" to listOf("meet", "see", "have"),
        "good to" to listOf("see", "know", "be"),
        "let me" to listOf("know", "see", "check"),
        "tell me" to listOf("what", "why", "how"),
        "give me" to listOf("a", "the", "some"),
        "send me" to listOf("a", "the", "that"),
        "see you" to listOf("soon", "later", "tomorrow"),
        "talk to" to listOf("you", "me", "him"),
        "wait for" to listOf("me", "you", "it"),
        "looking for" to listOf("a", "the", "you"),
        "do you" to listOf("want", "have", "know"),
        "are you" to listOf("okay", "sure", "ready"),
        "can you" to listOf("please", "help", "send"),
        "will you" to listOf("be", "come", "help"),
        "would you" to listOf("like", "please", "be"),
        "could you" to listOf("please", "help", "send"),
        "don't know" to listOf("what", "why", "if"),
        "do not" to listOf("know", "want", "have"),
        "it is" to listOf("a", "not", "so"),
        "this is" to listOf("a", "the", "so"),
        "that is" to listOf("so", "why", "true"),
        "there is" to listOf("a", "no", "one"),
        "there are" to listOf("no", "many", "some"),
        "we are" to listOf("going", "all", "here"),
        "you are" to listOf("the", "so", "welcome"),
        "they are" to listOf("not", "all", "here"),
        "so much" to listOf("for", "more", "better"),
        "a lot" to listOf("of", "more", "better"),
        "right now" to listOf("and", "I", "please"),
        "all the" to listOf("time", "way", "best"),
        "of the" to listOf("day", "year", "world"),
        "in the" to listOf("world", "morning", "evening"),
        "on the" to listOf("way", "phone", "table"),
        "at the" to listOf("moment", "same", "end"),
        "by the" to listOf("way", "time", "end"),
        "for the" to listOf("first", "best", "next"),
        "good morning" to listOf("everyone", "to", "my"),
        "good night" to listOf("everyone", "sweet", "sleep"),
        "happy birthday" to listOf("to", "my", "friend"),
    )

    private val nextWordBigrams: Map<String, List<String>> = mapOf(
        // Pronouns & starters
        "i" to listOf("am", "have", "think"),
        "i'm" to listOf("so", "good", "not"),
        "i've" to listOf("been", "got", "had"),
        "i'll" to listOf("be", "do", "send"),
        "i'd" to listOf("like", "love", "rather"),
        "you" to listOf("are", "can", "have"),
        "you're" to listOf("so", "right", "welcome"),
        "we" to listOf("are", "can", "have"),
        "we're" to listOf("going", "here", "ready"),
        "they" to listOf("are", "have", "will"),
        "they're" to listOf("not", "here", "coming"),
        "he" to listOf("is", "was", "has"),
        "he's" to listOf("not", "good", "here"),
        "she" to listOf("is", "was", "has"),
        "she's" to listOf("not", "good", "here"),
        "it" to listOf("is", "was", "will"),
        "it's" to listOf("a", "not", "so"),
        "this" to listOf("is", "was", "will"),
        "that" to listOf("is", "was", "would"),
        "that's" to listOf("so", "good", "right"),
        "there" to listOf("is", "are", "was"),
        "there's" to listOf("a", "no", "nothing"),
        "here" to listOf("is", "are", "you"),
        "here's" to listOf("the", "a", "my"),

        // Common verbs (the exact gap behind "I love ", "I want ", etc.)
        "love" to listOf("you", "it", "that"),
        "loves" to listOf("you", "it", "this"),
        "loved" to listOf("it", "you", "that"),
        "like" to listOf("that", "it", "this"),
        "liked" to listOf("it", "that", "this"),
        "hate" to listOf("it", "that", "when"),
        "miss" to listOf("you", "it", "this"),
        "want" to listOf("to", "it", "you"),
        "wanted" to listOf("to", "you", "this"),
        "wants" to listOf("to", "it", "you"),
        "need" to listOf("to", "it", "you"),
        "needed" to listOf("to", "it", "help"),
        "needs" to listOf("to", "a", "some"),
        "hope" to listOf("you", "so", "it"),
        "wish" to listOf("you", "I", "we"),
        "feel" to listOf("like", "so", "good"),
        "feels" to listOf("good", "like", "great"),
        "think" to listOf("so", "it", "that"),
        "know" to listOf("that", "what", "you"),
        "knew" to listOf("it", "that", "you"),
        "understand" to listOf("what", "why", "that"),
        "remember" to listOf("that", "when", "the"),
        "forget" to listOf("to", "about", "it"),
        "believe" to listOf("it", "you", "that"),
        "see" to listOf("you", "it", "what"),
        "saw" to listOf("you", "it", "that"),
        "look" to listOf("at", "like", "for"),
        "looking" to listOf("for", "at", "good"),
        "watch" to listOf("the", "this", "it"),
        "hear" to listOf("you", "that", "from"),
        "listen" to listOf("to", "up", "carefully"),
        "tell" to listOf("me", "you", "him"),
        "told" to listOf("me", "you", "him"),
        "ask" to listOf("you", "me", "for"),
        "say" to listOf("that", "hello", "goodbye"),
        "said" to listOf("that", "it", "he"),
        "talk" to listOf("to", "about", "soon"),
        "speak" to listOf("to", "with", "about"),
        "give" to listOf("me", "you", "it"),
        "gave" to listOf("me", "you", "it"),
        "send" to listOf("me", "it", "you"),
        "sent" to listOf("you", "me", "it"),
        "make" to listOf("it", "sure", "a"),
        "made" to listOf("it", "a", "me"),
        "making" to listOf("it", "a", "sure"),
        "take" to listOf("care", "it", "a"),
        "took" to listOf("a", "the", "me"),
        "get" to listOf("it", "the", "back"),
        "got" to listOf("it", "to", "a"),
        "getting" to listOf("a", "ready", "better"),
        "have" to listOf("a", "to", "you"),
        "has" to listOf("been", "a", "to"),
        "had" to listOf("a", "to", "been"),
        "having" to listOf("a", "fun", "trouble"),
        "keep" to listOf("it", "going", "the"),
        "let" to listOf("me", "us", "you"),
        "let's" to listOf("go", "do", "see"),
        "put" to listOf("it", "the", "that"),
        "use" to listOf("the", "it", "this"),
        "using" to listOf("the", "a", "this"),
        "find" to listOf("out", "the", "it"),
        "found" to listOf("out", "the", "it"),
        "show" to listOf("me", "you", "the"),
        "help" to listOf("you", "me", "with"),
        "try" to listOf("to", "it", "this"),
        "trying" to listOf("to", "my", "hard"),
        "wait" to listOf("for", "a", "here"),
        "waiting" to listOf("for", "to", "on"),
        "go" to listOf("to", "home", "back"),
        "going" to listOf("to", "home", "on"),
        "went" to listOf("to", "home", "out"),
        "come" to listOf("back", "here", "over"),
        "coming" to listOf("back", "soon", "home"),
        "came" to listOf("back", "to", "from"),
        "work" to listOf("on", "with", "today"),
        "working" to listOf("on", "now", "hard"),
        "play" to listOf("with", "the", "games"),
        "playing" to listOf("the", "with", "a"),
        "call" to listOf("you", "me", "later"),
        "check" to listOf("the", "it", "out"),
        "open" to listOf("the", "it", "a"),
        "close" to listOf("the", "it", "to"),
        "start" to listOf("the", "with", "now"),
        "stop" to listOf("it", "the", "now"),
        "save" to listOf("the", "it", "this"),
        "write" to listOf("a", "to", "it"),
        "read" to listOf("the", "this", "it"),
        "thank" to listOf("you", "god", "everyone"),
        "thanks" to listOf("for", "a", "so"),

        // Auxiliary & modal verbs
        "am" to listOf("so", "good", "sorry"),
        "is" to listOf("a", "the", "not"),
        "are" to listOf("you", "the", "we"),
        "was" to listOf("a", "so", "not"),
        "were" to listOf("you", "not", "all"),
        "be" to listOf("back", "there", "able"),
        "been" to listOf("good", "there", "busy"),
        "do" to listOf("you", "not", "it"),
        "doing" to listOf("good", "well", "great"),
        "did" to listOf("you", "not", "it"),
        "does" to listOf("it", "not", "anyone"),
        "don't" to listOf("know", "think", "worry"),
        "didn't" to listOf("know", "see", "have"),
        "can" to listOf("you", "I", "we"),
        "can't" to listOf("wait", "believe", "do"),
        "could" to listOf("you", "be", "have"),
        "couldn't" to listOf("find", "do", "believe"),
        "will" to listOf("be", "do", "send"),
        "won't" to listOf("be", "work", "have"),
        "would" to listOf("you", "be", "like"),
        "wouldn't" to listOf("be", "do", "want"),
        "should" to listOf("be", "we", "I"),
        "shouldn't" to listOf("be", "have", "do"),
        "must" to listOf("be", "have", "do"),

        // Question words & conjunctions
        "what" to listOf("is", "are", "do"),
        "what's" to listOf("up", "new", "wrong"),
        "how" to listOf("are", "to", "about"),
        "how's" to listOf("it", "everything", "your"),
        "why" to listOf("did", "are", "not"),
        "where" to listOf("are", "is", "did"),
        "when" to listOf("you", "will", "is"),
        "who" to listOf("is", "are", "can"),
        "which" to listOf("one", "is", "way"),
        "if" to listOf("you", "I", "it"),
        "because" to listOf("I", "it", "of"),
        "and" to listOf("I", "the", "you"),
        "but" to listOf("I", "it", "not"),
        "or" to listOf("not", "maybe", "just"),
        "so" to listOf("much", "good", "sorry"),

        // Adjectives, adverbs, greetings, determiners, prepositions
        "hello" to listOf("there", "everyone", "world"),
        "hi" to listOf("there", "everyone", "how"),
        "hey" to listOf("there", "how", "man"),
        "good" to listOf("morning", "night", "luck"),
        "great" to listOf("job", "idea", "day"),
        "happy" to listOf("to", "birthday", "for"),
        "sorry" to listOf("for", "about", "to"),
        "please" to listOf("send", "check", "let"),
        "yes" to listOf("I", "please", "sure"),
        "no" to listOf("problem", "worries", "way"),
        "okay" to listOf("cool", "thanks", "see"),
        "ok" to listOf("cool", "thanks", "sure"),
        "very" to listOf("good", "much", "nice"),
        "really" to listOf("good", "like", "nice"),
        "just" to listOf("wanted", "checking", "a"),
        "not" to listOf("sure", "bad", "yet"),
        "always" to listOf("be", "love", "remember"),
        "never" to listOf("mind", "had", "been"),
        "maybe" to listOf("we", "later", "tomorrow"),
        "right" to listOf("now", "here", "away"),
        "my" to listOf("friend", "name", "phone"),
        "your" to listOf("name", "phone", "time"),
        "our" to listOf("team", "new", "best"),
        "their" to listOf("own", "new", "best"),
        "his" to listOf("name", "own", "best"),
        "her" to listOf("name", "own", "best"),
        "a" to listOf("good", "lot", "new"),
        "an" to listOf("update", "hour", "email"),
        "the" to listOf("best", "same", "new"),
        "some" to listOf("time", "more", "help"),
        "more" to listOf("than", "time", "info"),
        "all" to listOf("the", "good", "day"),
        "in" to listOf("the", "a", "my"),
        "on" to listOf("the", "my", "this"),
        "for" to listOf("the", "you", "this"),
        "to" to listOf("the", "be", "do"),
        "of" to listOf("the", "my", "this"),
        "with" to listOf("you", "me", "the"),
        "from" to listOf("the", "my", "you"),
        "about" to listOf("it", "that", "you"),
        "at" to listOf("the", "home", "work"),
        "by" to listOf("the", "now", "tomorrow"),
        "into" to listOf("the", "a", "this"),
        "after" to listOf("the", "that", "work"),
        "before" to listOf("the", "we", "I"),
        "between" to listOf("the", "you", "us"),
    )

    fun extractContextWordsBeforeCursor(
        textBeforeCursor: String,
        activeWord: String = "",
    ): Pair<String, String> {
        if (textBeforeCursor.isEmpty()) return "" to ""
        var prefix = textBeforeCursor
        if (activeWord.isNotEmpty() && prefix.endsWith(activeWord, ignoreCase = false)) {
            prefix = prefix.dropLast(activeWord.length)
        }
        val trimmedEnd = prefix.trimEnd { it == ' ' || it == '\t' }
        if (trimmedEnd.isEmpty()) return "" to ""
        val lastChar = trimmedEnd.last()
        if (lastChar == '.' || lastChar == '!' || lastChar == '?' || lastChar == '\n' || lastChar == ';' || lastChar == ':') {
            return "" to ""
        }
        // Scan backwards within the current sentence clause
        var clauseStart = 0
        for (i in trimmedEnd.length - 1 downTo 0) {
            val c = trimmedEnd[i]
            if (c == '.' || c == '!' || c == '?' || c == '\n' || c == ';' || c == ':') {
                clauseStart = i + 1
                break
            }
        }
        val clause = trimmedEnd.substring(clauseStart)
        val tokens = Regex("[A-Za-z][A-Za-z']*").findAll(clause).map { it.value }.toList()
        return when {
            tokens.isEmpty() -> "" to ""
            tokens.size == 1 -> "" to tokens[0]
            else -> tokens[tokens.size - 2] to tokens[tokens.size - 1]
        }
    }

    private fun contextualNextWords(
        lastCommittedWord: String,
        prevCommittedWord: String,
    ): List<String> {
        val lastLower = lastCommittedWord.trim().lowercase(Locale.US)
        if (lastLower.isEmpty()) return defaultStarters
        val prevLower = prevCommittedWord.trim().lowercase(Locale.US)
        if (prevLower.isNotEmpty()) {
            trigramPredictions["$prevLower $lastLower"]?.let { return it }
        }
        nextWordBigrams[lastLower]?.let { return it }
        return midSentenceFallbacks
            .filterNot { it.equals(lastLower, ignoreCase = true) }
            .ifEmpty { defaultStarters }
    }

    fun isKnownWord(word: String): Boolean {
        if (word.isBlank()) return false
        return wordRankMap.containsKey(word.lowercase(Locale.US))
    }

    fun evaluate(
        activeWord: String,
        lastCommittedWord: String = "",
        prevCommittedWord: String = "",
        learnedWords: Set<String> = emptySet(),
        autoCorrectEnabled: Boolean = true,
        eligibleForAutoCorrect: Boolean = true,
    ): SuggestionEvalResult {
        val contextCandidates = contextualNextWords(lastCommittedWord, prevCommittedWord)
        val trimmed = activeWord.trim()
        if (trimmed.isEmpty()) {
            return SuggestionEvalResult(
                slots = contextCandidates.take(3).map { SuggestionSlot(text = it, commitTextValue = it) },
                autoCorrectReplacement = null,
            )
        }

        val lower = trimmed.lowercase(Locale.US)
        val isAllCaps = trimmed.length >= 2 && trimmed.all { !it.isLetter() || it.isUpperCase() }
        val isPureWord = lower.all { it in 'a'..'z' || it == '\'' }
        val isLearned = lower in learnedWords
        val exactRank = wordRankMap[lower]
        val isExactKnown = exactRank != null || isLearned

        val contextPrefixMatches = contextCandidates
            .asSequence()
            .filter {
                val wl = it.lowercase(Locale.US)
                wl.startsWith(lower) && wl != lower
            }
            .toList()

        val learnedPrefixMatches = learnedWords
            .asSequence()
            .filter { it.startsWith(lower) && it != lower }
            .take(3)
            .toList()

        val prefixMatches = commonWordsOrdered
            .asSequence()
            .filter {
                val wl = it.lowercase(Locale.US)
                wl.startsWith(lower) && wl != lower
            }
            .take(6)
            .toList()

        // Explicit typo shortcut (e.g. "teh" -> "the", "dont" -> "don't")
        val directTypo = if (!isExactKnown && isPureWord && !isAllCaps) {
            commonTypos[lower]
        } else {
            null
        }

        val fuzzyMatches = if (isPureWord && lower.length >= 2 && !isAllCaps) {
            findBestFuzzyMatches(lower, maxEditDist = if (lower.length >= 5) 2 else 1)
        } else {
            emptyList()
        }

        val rawAutoCandidate: String? = when {
            !autoCorrectEnabled || !eligibleForAutoCorrect || isExactKnown || isAllCaps || !isPureWord -> null
            lower.length < 2 -> null
            directTypo != null -> applyWordCase(trimmed, directTypo)
            else -> {
                val top = fuzzyMatches.firstOrNull()
                if (top != null && isConfidentAutoCorrect(lower, top.word, top.distance)) {
                    applyWordCase(trimmed, top.word)
                } else {
                    null
                }
            }
        }

        val orderedSuggestions = linkedSetOf<String>()
        if (rawAutoCandidate != null) {
            orderedSuggestions.add(rawAutoCandidate)
        }
        contextPrefixMatches.forEach { w ->
            orderedSuggestions.add(applyWordCase(trimmed, w))
        }
        learnedPrefixMatches.forEach { w ->
            orderedSuggestions.add(applyWordCase(trimmed, w))
        }
        prefixMatches.forEach { w ->
            orderedSuggestions.add(applyWordCase(trimmed, w))
        }
        fuzzyMatches.forEach { m ->
            orderedSuggestions.add(applyWordCase(trimmed, m.word))
        }

        val slots = mutableListOf<SuggestionSlot>()
        if (!isExactKnown) {
            // Left slot always keeps the exact word typed by user (tap saves it as learned custom word)
            slots.add(
                SuggestionSlot(
                    text = trimmed,
                    isLiteralTyped = true,
                    isAutoCorrectHighlight = false,
                    commitTextValue = trimmed,
                ),
            )
            val candidates = orderedSuggestions
                .filterNot { it.equals(trimmed, ignoreCase = true) }
                .take(2)
            candidates.forEachIndexed { idx, cand ->
                slots.add(
                    SuggestionSlot(
                        text = cand,
                        isLiteralTyped = false,
                        isAutoCorrectHighlight = idx == 0 && rawAutoCandidate != null,
                        commitTextValue = cand,
                    ),
                )
            }
        } else {
            val candidates = orderedSuggestions
                .filterNot { it.equals(trimmed, ignoreCase = true) }
                .take(2)
            if (candidates.isNotEmpty()) {
                slots.add(
                    SuggestionSlot(
                        text = trimmed,
                        isLiteralTyped = true,
                        isAutoCorrectHighlight = false,
                        commitTextValue = trimmed,
                    ),
                )
                candidates.forEach { cand ->
                    slots.add(
                        SuggestionSlot(
                            text = cand,
                            isLiteralTyped = false,
                            isAutoCorrectHighlight = false,
                            commitTextValue = cand,
                        ),
                    )
                }
            } else {
                slots.add(
                    SuggestionSlot(
                        text = trimmed,
                        isLiteralTyped = true,
                        isAutoCorrectHighlight = false,
                        commitTextValue = trimmed,
                    ),
                )
            }
        }

        // Pad up to 3 slots cleanly so the 3-slot bar never collapses unevenly
        val fallbackPool = contextCandidates + midSentenceFallbacks + defaultStarters + commonWordsOrdered.take(12)
        for (fb in fallbackPool) {
            if (slots.size >= 3) break
            if (slots.none { it.text.equals(fb, ignoreCase = true) }) {
                slots.add(SuggestionSlot(text = fb, commitTextValue = fb))
            }
        }

        return SuggestionEvalResult(
            slots = slots.take(3),
            autoCorrectReplacement = rawAutoCandidate,
        )
    }

    private data class FuzzyCandidate(
        val word: String,
        val distance: Int,
        val score: Float,
    )

    private fun findBestFuzzyMatches(lowerInput: String, maxEditDist: Int): List<FuzzyCandidate> {
        val len = lowerInput.length
        val results = mutableListOf<FuzzyCandidate>()
        for ((index, dictWord) in commonWordsOrdered.withIndex()) {
            val w = dictWord.lowercase(Locale.US)
            if (w == lowerInput) continue
            if (abs(w.length - len) > maxEditDist) continue
            // Quick bound check for length >= 4: share at least one character in first 2 chars when dist == 1
            val dist = damerauLevenshtein(lowerInput, w, maxEditDist) ?: continue
            if (dist <= maxEditDist) {
                val freqBonus = ((commonWordsOrdered.size - index).toFloat() / commonWordsOrdered.size) * 2.2f
                val prefixPenalty = if (w.firstOrNull() != lowerInput.firstOrNull()) 0.7f else 0f
                val proxAdj = substitutionProximityPenalty(lowerInput, w)
                val score = dist * 4.0f + prefixPenalty + proxAdj - freqBonus
                results.add(FuzzyCandidate(word = dictWord, distance = dist, score = score))
            }
        }
        return results.sortedBy { it.score }.take(4)
    }

    private fun isConfidentAutoCorrect(lowerInput: String, candidate: String, distance: Int): Boolean {
        val w = candidate.lowercase(Locale.US)
        if (lowerInput.length <= 2 && distance > 1) return false
        if (lowerInput.length == 2) {
            // Only swap 2-letter words if same letters transposed (e.g. "si" shouldn't stomp randomly; keep conservative)
            return lowerInput.toSet() == w.toSet() && distance == 1
        }
        if (distance == 1) return true
        // Distance == 2 allowed only for longer words sharing the first letter and strong frequency
        return lowerInput.length >= 5 &&
            w.firstOrNull() == lowerInput.firstOrNull() &&
            (wordRankMap[w] ?: Int.MAX_VALUE) < 900
    }

    private fun substitutionProximityPenalty(a: String, b: String): Float {
        if (a.length != b.length) return 0.35f
        var diffIdx = -1
        var diffs = 0
        for (i in a.indices) {
            if (a[i] != b[i]) {
                diffIdx = i
                diffs++
                if (diffs > 1) return 0.25f
            }
        }
        if (diffs != 1 || diffIdx < 0) return 0f
        val p1 = qwertyCoords[a[diffIdx]] ?: return 0.6f
        val p2 = qwertyCoords[b[diffIdx]] ?: return 0.6f
        val dx = p1.first - p2.first
        val dy = p1.second - p2.second
        val distSq = dx * dx + dy * dy
        return if (distSq <= 2.2f) -0.45f else 0.55f
    }

    private fun damerauLevenshtein(a: String, b: String, maxDist: Int): Int? {
        val m = a.length
        val n = b.length
        if (abs(m - n) > maxDist) return null
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) {
            var rowMin = Int.MAX_VALUE
            for (j in 1..n) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var best = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost,
                )
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    best = min(best, dp[i - 2][j - 2] + 1)
                }
                dp[i][j] = best
                if (best < rowMin) rowMin = best
            }
            if (rowMin > maxDist) return null
        }
        val finalDist = dp[m][n]
        return if (finalDist <= maxDist) finalDist else null
    }

    internal fun applyWordCase(typed: String, suggestion: String): String {
        if (suggestion.isEmpty()) return suggestion
        // Respect explicit mixed-case contractions like I'm, I've, I'll
        if (suggestion.startsWith("I'")) {
            return if (typed.all { !it.isLetter() || it.isUpperCase() } && typed.length >= 2) {
                suggestion.uppercase(Locale.US)
            } else {
                suggestion
            }
        }
        val letters = typed.filter { it.isLetter() }
        return when {
            letters.length >= 2 && letters.all { it.isUpperCase() } -> suggestion.uppercase(Locale.US)
            letters.firstOrNull()?.isUpperCase() == true ->
                suggestion.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
            else -> suggestion
        }
    }

    // Top ~1,200 high-frequency everyday English words ranked by frequency + contractions & common tech words
    private const val COMMON_WORDS_RAW =
        "the be to of and a in that have I it for not on with he as you do at this but his by from " +
            "they we say her she or an will my one all would there their what so up out if about who get " +
            "which go me when make can like time no just him know take people into year your good some could " +
            "them see other than then now look only come its over think also back after use two how our work " +
            "first well way even new want because any these give day most us is are was were been has had " +
            "did does doing made said says got went came saw knew thought took gave found told felt left " +
            "hello hi hey ok okay yes yeah yep nope thanks thank please sorry welcome morning night today " +
            "tomorrow yesterday always never sometimes maybe really very much more many few little big " +
            "small large long short high low right wrong true false same different next last every each " +
            "much great nice cool awesome fine bad best better worse worst happy sad angry tired busy " +
            "free open close start stop send save load edit delete copy paste cut undo redo search find " +
            "help need try keep let put show tell ask call talk speak read write listen watch play run " +
            "walk move turn hold press tap click type enter space shift ctrl alt tab esc win home end " +
            "back up down left right top bottom center middle side line row page screen app phone mobile " +
            "device android tablet keyboard desktop window browser link file folder text note message chat " +
            "email mail photo image picture video audio sound music voice game play level score win lose " +
            "team player friend family mother father brother sister son daughter wife husband child kids " +
            "baby man woman boy girl guy person name word letter number code data list item set option " +
            "setting theme color dark light black white gray grey blue red green yellow orange purple pink " +
            "house home room door table chair bed car bus train plane road street city town country world " +
            "earth water fire air sun moon star rain snow wind food drink coffee tea bread rice meat fruit " +
            "money bank card cash price cost buy sell pay shop store market order check bill account login " +
            "password user profile share export import backup update install download upload build test fix " +
            "error issue bug crash fast slow quick easy hard simple full empty clean soft sharp clear " +
            "already still yet soon late early later again almost enough quite rather pretty too such own " +
            "while where why how whose whom though although since until unless whether either neither both " +
            "between among through during before above below around near far inside outside behind front " +
            "across along toward away off down against without within under upon per via etc " +
            "am being having doing going getting making knowing thinking taking seeing coming wanting looking " +
            "using finding giving telling working calling trying asking needing feeling becoming leaving " +
            "putting meaning keeping letting beginning seeming helping talking turning starting showing " +
            "hearing playing running moving living believing bringing happening writing providing sitting " +
            "standing losing paying meeting including continuing setting learning changing leading " +
            "understanding watching following stopping creating speaking reading allowing adding spending " +
            "growing opening walking winning offering remembering loving considering appearing buying waiting " +
            "serving dying sending expecting building staying falling cutting reaching killing remaining " +
            "suggesting raising passing selling requiring reporting deciding pulling breaking carrying " +
            "checking choosing cleaning closing collecting comparing completing confirming connecting " +
            "containing copying counting covering creating cutting dealing deciding delivering describing " +
            "designing developing discussing drawing driving dropping eating enabling ending enjoying " +
            "entering escaping explaining exploring extending facing failing fearing fighting filling " +
            "finishing fixing flying forgetting forgiving forming gaining gathering guessing handling " +
            "hanging hating hiding hitting holding hoping hurting imagining improving increasing " +
            "indicating informing inserting installing intending introducing joining jumping keeping " +
            "kicking kissing knocking knowing laughing laying leading leaning learning leaving lending " +
            "letting lifting lighting liking listening living loading locking looking losing loving " +
            "managing marking matching mattering meaning measuring meeting mentioning minding missing " +
            "mixing moving naming needing noting noticing obtaining occurring offering opening ordering " +
            "owning packing painting parking passing paying performing picking placing planning playing " +
            "pointing pouring practicing preferring preparing presenting pressing pretending preventing " +
            "printing producing promising protecting proving providing pulling pushing putting raising " +
            "reaching reacting reading realizing receiving recognizing recommending recording reducing " +
            "referring reflecting refusing regarding relating relaxing releasing relying remaining " +
            "remembering reminding removing repairing repeating replacing replying reporting representing " +
            "requiring resting returning revealing riding ringing rising rolling running rushing saving " +
            "saying scoring searching seeing seeming selecting selling sending sensing separating serving " +
            "setting settling shaking sharing shooting shopping shouting showing shutting signing singing " +
            "sitting sleeping sliding smiling smoking sorting sounding speaking spending splitting " +
            "spreading standing staring starting stating staying stealing stepping sticking stopping " +
            "storing studying succeeding suffering suggesting supplying supporting supposing surprising " +
            "surviving swimming switching taking talking tasting teaching tearing telling testing thanking " +
            "thinking throwing touching tracking trading training transferring traveling treating trying " +
            "turning typing understanding uniting updating upgrading uploading using visiting waiting " +
            "waking walking wanting warning washing watching wearing winning wiping wishing wondering " +
            "working worrying writing yelling able actually afraid almost alone already alright also " +
            "amazing ancient another anyone anything anywhere apparently around available awesome awful " +
            "basic beautiful became become becomes began begin beginning behind believe below beneath " +
            "beside besides better between beyond billion bit black blood blue board body book born " +
            "bottom bought box break bring broke broken brought build built business busy button " +
            "camera cancel cannot careful carefully carry cause center certain certainly chance change " +
            "check chief choice choose chose chosen church circle class clean clear clearly close closed " +
            "closer clothes cloud coffee cold collect color come comes comfort common company compare " +
            "complete completely computer concern condition consider continue control cool copy corner " +
            "correct cost could count country couple course court cover create cross crowd cry cup " +
            "current currently custom cut daily damage dance danger dark data date daughter dead deal " +
            "dear death decide decision deep deeply defense degree deliver demand depend describe design " +
            "detail determine develop device difference different difficult dinner direct direction directly " +
            "discover discuss discussion disease distance divide doctor document dog dollar done double " +
            "doubt down dozen draw dream dress drink drive drop drug dry due during dust duty each " +
            "early earth east easy eat edge editor education effect effective effort eight either else " +
            "empty enable end energy enjoy enough enter entire environment equal especially establish " +
            "even evening event ever every everybody everyone everything everywhere evidence exact " +
            "exactly example except excite exciting exist expect experience explain express extra eye " +
            "face fact factor fail fair fall false familiar family famous far farm fast fat father " +
            "fear feature feed feel feeling feet fell fellow felt female few field fight figure fill " +
            "film final finally finance financial find fine finger finish fire firm first fish fit " +
            "five fix flat floor flow flower fly focus follow food foot football force foreign forest " +
            "forget form formal former forward found four fourth frame free freedom fresh friend from " +
            "front full fun function fund funny future gain game garden gas gather general generally " +
            "gentle get gets girl give given gives glad glass global go goal god goes going gold gone " +
            "good got government grand grant grass great green ground group grow growth guess guest " +
            "guide gun guy habit hair half hall hand handle hang happen happened happy hard hardly " +
            "harm hat hate have having he head health hear heard heart heat heavy held hell hello help " +
            "helpful her here herself hi hide high highly hill him himself his history hit hold hole " +
            "holiday home honest hope horse hospital hot hotel hour house how however huge human " +
            "hundred hung hungry hunt hurry hurt husband ice idea ideal identify ignore ill image " +
            "imagine immediate immediately impact important impossible improve in include including " +
            "income increase indeed indicate individual industry info information inside insist install " +
            "instance instead intend interest interesting internal international into introduce invent " +
            "invest invite involve iron is island issue it item its itself jacket job join joint joke " +
            "journey joy judge jump just keep kept key keyboard kick kid kill kind king kiss kitchen " +
            "knee knew knife knock know knowledge known label lack lady laid lake land language large " +
            "last late later latest laugh launch law lawyer lay layer lead leader learn least leather " +
            "leave led left leg legal less lesson let letter level library lie life lift light like " +
            "likely limit line link lip list listen little live load local lock log lone long look " +
            "lord lose loss lost lot loud love low lower luck lunch machine mad made magazine magic " +
            "mail main mainly maintain major majority make maker makes making male man manage manager " +
            "manner many map march mark market marriage marry mass master match material matter may " +
            "maybe mayor me meal mean meaning meant measure media medical meet meeting member memory " +
            "men mention menu merely message metal method middle might mile military milk million mind " +
            "mine minor minute mirror miss mistake mix mobile model modern moment money month mood " +
            "moon more morning most mostly mother motion motor mountain mouse mouth move movement movie " +
            "much multiple murder muscle music must my myself mystery name narrow nation national " +
            "natural nature near nearly neat necessary neck need negative neighbor neither network never " +
            "new news newspaper next nice night nine no nobody nod noise none nor normal normally north " +
            "nose not note nothing notice notion noun now nowhere nuclear number numerous nurse object " +
            "obtain obvious obviously occasion occupy occur ocean odd of off offer office officer " +
            "official often oh oil ok okay old on once one only onto open operate operation opinion " +
            "opportunity oppose opposite option or orange order ordinary other others otherwise ought " +
            "our ours ourselves out outcome outside over overall overcome owe own owner pace pack " +
            "package page paid pain paint pair pale palm pan panel panic paper parent park part " +
            "particular particularly partly partner party pass past path patient pattern pause pay " +
            "payment peace peak pen pencil people per percent perfect perfectly perform performance " +
            "perhaps period permanent permit person personal personality persuade pet phase phone photo " +
            "phrase physical piano pick picture piece pig pile pilot pin pink pipe pitch place plain " +
            "plan plane planet plant plastic plate platform play player please pleasure plenty plot " +
            "plus pocket poem poet point police policy political politics poll pollute poor pop popular " +
            "population porch port portion position positive possess possible possibly post pot potato " +
            "potential pound pour powder power powerful practical practice praise pray prayer prefer " +
            "prepare presence present preserve press pressure pretend pretty prevent previous previously " +
            "price pride primary prime prince principal principle print prior priority prison private " +
            "prize probably problem proceed process produce product production profession professional " +
            "professor profile profit program progress project promise promote prompt proof proper " +
            "properly property proportion propose protect protection proud prove provide province " +
            "public pull pump punch punish pupil purchase pure purpose push put qualify quality " +
            "quarter queen question quick quickly quiet quietly quit quite quote race radio rail rain " +
            "raise range rank rapid rapidly rare rarely rate rather ratio raw reach react reaction " +
            "read reader ready real reality realize really reason reasonable recall receive recent " +
            "recently recognize recommend record recover red reduce refer reflect reform refuse regard " +
            "region register regret regular regularly reject relate relation relationship relative " +
            "relax release relevant reliable relief rely remain remark remember remind remote remove " +
            "rent repair repeat replace reply report represent request require research resemble " +
            "reserve resist resolve resort resource respect respond response responsibility responsible " +
            "rest restaurant restore restrict result retain retire return reveal review reward rhythm " +
            "rice rich rid ride right ring rise risk river road rock role roll roof room root rope " +
            "rose rough roughly round route routine row royal rub rubber rude ruin rule run rush sad " +
            "safe safely safety said sail sake salad salary sale salt same sample sand sandwich satisfy " +
            "sauce save saw say scale scared scene schedule scheme school science scientific scientist " +
            "scope score scratch scream screen script sea search season seat second secret section " +
            "sector secure security see seed seek seem seen seize select selection self sell send " +
            "senior sense sensitive sentence separate sequence series serious seriously servant serve " +
            "service session set settle seven several severe sex shade shadow shake shall shallow shame " +
            "shape share sharp she sheet shelf shell shelter shift shine ship shirt shock shoe shoot " +
            "shop shore short shortly shot should shoulder shout show shower shut sick side sigh sight " +
            "sign signal signature significant significantly silence silent silk silly silver similar " +
            "similarly simple simply since sing single sink sir sister sit site situation six size " +
            "skill skin skirt sky slave sleep sleeve slice slide slight slightly slip slow slowly small " +
            "smart smell smile smoke smooth snap snow so soap soccer social society sock soft softly " +
            "software soil solar sold soldier solid solution solve some somebody somehow someone " +
            "something sometimes somewhat somewhere son song soon sorry sort soul sound soup source " +
            "south southern space spare speak speaker special specialist specific specifically speech " +
            "speed spell spend spent spin spirit split spoke spoken sport spot spread spring square " +
            "squeeze stable staff stage stair stake stamp stand standard star stare start state " +
            "statement station status stay steady steal steam steel steep stem step stick stiff still " +
            "stir stock stomach stone stood stop storage store storm story straight strange stranger " +
            "strategy stream street strength stress stretch strict strictly strike string strip stroke " +
            "strong strongly structure struggle student studio study stuff stupid style subject submit " +
            "subsequent substance succeed success successful successfully such suck sudden suddenly " +
            "suffer sufficient sugar suggest suggestion suit suitable summer sun super supply support " +
            "suppose sure surely surface surgery surprise surprised surprising surround survey survive " +
            "suspect sustain swallow swear sweep sweet swim swing switch symbol symptom system table " +
            "tablet tackle tail take taken takes taking tale talent talk tall tank tap tape target task " +
            "taste tax tea teach teacher team tear technical technique technology teen teenager " +
            "telephone television tell temperature temporary ten tend tendency tennis tent term terms " +
            "terrible test text than thank thanks that the their theirs them themselves then theory " +
            "therapy there therefore these they thick thin thing think thinking third thirsty thirteen " +
            "thirty this thorough thoroughly those though thought thousand thread threat threaten three " +
            "threw throat through throughout throw thrown thumb thus ticket tide tie tight tightly " +
            "till time tiny tip tire tired tissue title to today toe together toilet told tomorrow tone " +
            "tongue tonight too took tool tooth top topic total totally touch tough tour toward towards " +
            "tower town toy trace track trade tradition traditional traffic train training transfer " +
            "transform transition translate transport trap trash travel tray treat treatment tree " +
            "tremendous trend trial trick tried tries trip troop trouble truck true truly trust truth " +
            "try tube tune tunnel turn twelve twenty twice twin two type typical typically ugly " +
            "ultimate ultimately unable uncle under undergo understand understanding undertake undo " +
            "unexpected unfortunately uniform union unique unit unite universal universe university " +
            "unknown unless unlike unlikely until unusual up update upgrade upon upper upset upstairs " +
            "urban urge us usage use used useful user uses using usual usually utility vacation vague " +
            "valid valley valuable value van variety various vary vast vegetable vehicle venture " +
            "version versus very vessel veteran via victim victory video view viewer village violate " +
            "violence violent virtual virtually virus visible vision visit visitor visual vital voice " +
            "volume volunteer vote vs wage waist wait wake walk wall wallet wander want war warm warn " +
            "warning wash waste watch water wave way weak weakness wealth wealthy weapon wear weather " +
            "web website wedding week weekend weekly weigh weight weird welcome well went were west " +
            "western wet what whatever wheel when whenever where whereas wherever whether which while " +
            "whisper white who whoever whole whom whose why wide widely widespread wife wild will " +
            "willing win wind window wine wing winner winter wipe wire wisdom wise wish with withdraw " +
            "within without witness wolf woman women won wonder wonderful wood wooden wool word work " +
            "worker working works workshop world worried worry worse worst worth would wound wrap " +
            "wrist write writer writing written wrong wrote yard yeah year yell yellow yes yesterday " +
            "yet yield you young your yours yourself yourselves youth zero zone " +
            "I'm I've I'll I'd don't can't won't didn't isn't aren't doesn't haven't hasn't hadn't " +
            "wasn't weren't wouldn't couldn't shouldn't you're we're they're that's what's here's " +
            "there's let's it's he's she's who's how's where's when's why's"
}

/**
 * Tracks only freshly typed forward characters for Simple Mode 3-suggestions and strict
 * one-time autocorrect:
 * - Never autocorrects a word that was backspaced/edited or clicked back into.
 * - Backspace right after autocorrection reverts to the original typed word and permanently
 *   learns it for the session so KeyDeck never overrides the user's custom word again.
 */
class SimpleTypingController {
    var activeWord: String = ""
        private set

    var lastCommittedWord: String = ""
        private set

    var prevCommittedWord: String = ""
        private set

    var wordEligibleForAutoCorrect: Boolean = false
        private set

    var pendingUndo: PendingAutoCorrectUndo? = null
        private set

    private val _learnedCustomWords = mutableSetOf<String>()
    val learnedCustomWords: Set<String> get() = _learnedCustomWords

    fun currentEval(autoCorrectEnabled: Boolean): SuggestionEvalResult {
        return SimpleSuggestionEngine.evaluate(
            activeWord = activeWord,
            lastCommittedWord = lastCommittedWord,
            prevCommittedWord = prevCommittedWord,
            learnedWords = _learnedCustomWords,
            autoCorrectEnabled = autoCorrectEnabled,
            eligibleForAutoCorrect = wordEligibleForAutoCorrect,
        )
    }

    fun syncContextFromSurroundingText(beforeCursor: String) {
        val (prev, last) = SimpleSuggestionEngine.extractContextWordsBeforeCursor(
            textBeforeCursor = beforeCursor,
            activeWord = activeWord,
        )
        prevCommittedWord = prev
        lastCommittedWord = last
    }

    /**
     * Call when the user taps a character in Simple Mode without Ctrl/Alt/Meta.
     * [isCursorTouchingExistingWord] should be true if activeWord was empty and the cursor
     * is currently adjacent to/inside existing letters/digits (editing an older word).
     */
    fun onCharTyped(
        ch: Char,
        autoCorrectEnabled: Boolean,
        isCursorTouchingExistingWord: Boolean = false,
    ): AutoCorrectAction? {
        if (isWordChar(ch)) {
            if (activeWord.isEmpty()) {
                // Only eligible if starting cleanly (not editing an existing/previous word)
                wordEligibleForAutoCorrect = !isCursorTouchingExistingWord
            }
            pendingUndo = null
            activeWord += ch
            return null
        }

        // Separator (space or sentence punctuation)
        val isAutoCorrectTrigger = ch == ' ' || ch in SENTENCE_TRIGGERS
        val isSentenceEnd = ch in SENTENCE_END_PUNCT
        if (isAutoCorrectTrigger && activeWord.isNotEmpty()) {
            val eval = SimpleSuggestionEngine.evaluate(
                activeWord = activeWord,
                lastCommittedWord = lastCommittedWord,
                prevCommittedWord = prevCommittedWord,
                learnedWords = _learnedCustomWords,
                autoCorrectEnabled = autoCorrectEnabled,
                eligibleForAutoCorrect = wordEligibleForAutoCorrect,
            )
            val candidate = eval.autoCorrectReplacement
            if (candidate != null && !candidate.equals(activeWord, ignoreCase = false)) {
                val original = activeWord
                val replacementWithTrigger = candidate + ch
                pendingUndo = PendingAutoCorrectUndo(
                    originalWord = original,
                    replacedTextWithSeparator = replacementWithTrigger,
                )
                if (isSentenceEnd) {
                    prevCommittedWord = ""
                    lastCommittedWord = ""
                } else {
                    prevCommittedWord = lastCommittedWord
                    lastCommittedWord = candidate
                }
                activeWord = ""
                wordEligibleForAutoCorrect = false
                return AutoCorrectAction(
                    originalWord = original,
                    replacementWithSeparator = replacementWithTrigger,
                )
            }
        }

        if (isSentenceEnd) {
            prevCommittedWord = ""
            lastCommittedWord = ""
        } else if (activeWord.isNotEmpty()) {
            prevCommittedWord = lastCommittedWord
            lastCommittedWord = activeWord
        } else if (ch != ' ') {
            prevCommittedWord = ""
            lastCommittedWord = ""
        }
        pendingUndo = null
        activeWord = ""
        wordEligibleForAutoCorrect = false
        return null
    }

    /**
     * Handles Backspace. Returns a [PendingAutoCorrectUndo] when the user immediately backspaces
     * after an autocorrection, instructing the caller to revert to [PendingAutoCorrectUndo.originalWord]
     * and learn it. Otherwise disarms autocorrect for the edited word.
     */
    fun onBackspace(): PendingAutoCorrectUndo? {
        val undo = pendingUndo
        if (undo != null) {
            pendingUndo = null
            _learnedCustomWords.add(undo.originalWord.lowercase(Locale.US))
            // Keep editing disabled so autocorrect won't re-trigger on Space for this word
            activeWord = undo.originalWord
            wordEligibleForAutoCorrect = false
            return undo
        }
        // Any manual backspace inside or back into a word means the user is editing -> lock autocorrect off!
        wordEligibleForAutoCorrect = false
        if (activeWord.isNotEmpty()) {
            activeWord = activeWord.dropLast(1)
        }
        return null
    }

    /**
     * Call on cursor movement (Spacebar slide, arrows, Home/End, Enter, shortcuts, input reset).
     * Disarms active autocorrect so moving back to edit an old word never triggers it.
     */
    fun resetOnCursorOrBoundary(clearLastWord: Boolean = true) {
        activeWord = ""
        pendingUndo = null
        wordEligibleForAutoCorrect = false
        if (clearLastWord) {
            prevCommittedWord = ""
            lastCommittedWord = ""
        }
    }

    /**
     * Call when the user taps one of the 3 suggestion chips.
     * Returns the full replacement string (with trailing space) if `activeWord` should be replaced,
     * or just commits the chip word + space if `activeWord` was empty.
     */
    fun acceptSuggestion(slot: SuggestionSlot): Pair<String, String> {
        val prior = activeWord
        if (slot.isLiteralTyped && prior.isNotBlank()) {
            _learnedCustomWords.add(prior.lowercase(Locale.US))
        }
        val toCommit = slot.commitTextValue + " "
        prevCommittedWord = lastCommittedWord
        lastCommittedWord = slot.commitTextValue
        activeWord = ""
        pendingUndo = null
        wordEligibleForAutoCorrect = false
        return prior to toCommit
    }

    private fun isWordChar(ch: Char): Boolean {
        return ch.isLetter() || ch == '\''
    }

    companion object {
        private val SENTENCE_TRIGGERS = charArrayOf('.', ',', '!', '?', ';', ':')
        private val SENTENCE_END_PUNCT = charArrayOf('.', '!', '?', '\n')
    }
}
