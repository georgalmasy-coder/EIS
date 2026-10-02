# Traceability text matching

Possible relations use only requirement **name and description**. Codes/IDs are
excluded. Confirmed and not-relevant relations take precedence over suggestions.

The following optional entries in `eis-global.properties` show the defaults:

```properties
traceability.match.fuzzy.enabled=true
traceability.match.minimum-matching-words=2
traceability.match.stopwords=og,i,jeg,det,at,en,den,til,er,som,p\u00e5,de,med,han,af,for,der,var,mig,sig,men,et,har,om,vi,the,and,a,an,of,to,in,is,it,that,for,on,with,as,was,are,be,by,this,from,or,at,have,has,they
traceability.match.fuzzy.short-word-max-length=4
traceability.match.fuzzy.medium-word-max-length=8
traceability.match.fuzzy.medium-word-max-edits=1
traceability.match.fuzzy.long-word-max-edits=2
```

Existing configuration files do not need these entries: missing values use the
defaults. Newly generated files include them. The existing configuration listener
reloads changes approximately every five seconds; reload the matrix afterwards.
Settings and prepared requirement words are retained for each matrix generation.

Words are case-insensitive Unicode letter/number sequences. Punctuation separates
words. Repeated words count once across name and description, and each word on
either side can participate in at most one match. Exact whole-word matches are
counted first. If needed, fuzzy matching uses Levenshtein distance (insertions,
deletions and substitutions), with the edit limit chosen by the shorter word.
Words up to four characters require exact matches by default. No language-specific
stemming or synonym matching is applied. Configured stopwords are removed from
both requirements before exact and fuzzy matching.

The default stopword selection contains 25 common Danish and 25 common English
words from [NLTK's stopword corpus](https://github.com/nltk/nltk_data/blob/gh-pages/packages/corpora/stopwords.zip).
These are common-word selections, not a measured frequency ranking for requirement
texts. The combined list has 48 distinct words because `at` and `for` appear in
both languages.

- Danish: og, i, jeg, det, at, en, den, til, er, som, på, de, med, han, af, for,
  der, var, mig, sig, men, et, har, om, vi.
- English: the, and, a, an, of, to, in, is, it, that, for, on, with, as, was, are,
  be, by, this, from, or, at, have, has, they.

`traceability.match.stopwords` is a comma-separated replacement for the default
list, not an addition to it. It ignores case, surrounding whitespace and duplicate
entries. An absent property uses the default list; an explicitly empty value
disables stopword filtering. Stopwords are excluded from both the yellow-cell
calculation and dialog highlights. Other words containing a stopword are retained.
The existing Java properties loader uses ISO-8859-1; in UTF-8-edited properties
files, write `på` as `p\u00e5` (as in the example above).

The Traceability Details dialog displays all matched word pairs in bold in both
names and descriptions, including matches below the yellow-cell threshold. It
uses the server matcher with the current configuration when the dialog opens.
Repeated occurrences are all highlighted but count as one word pair. Text is
rendered as plain text, preserving spelling and line breaks.

Integer ranges: minimum matching words 1–1000; word-length thresholds 1–100;
edit limits 0–5. Invalid/out-of-range values use their defaults. The effective
medium-word length threshold is at least the short-word threshold.

Run the standalone regression suite after compiling:

```powershell
javac -encoding UTF-8 -cp 'web-server/target/classes;common/target/classes' -d web-server/target/test-classes web-server/src/test/java/com/bepa/eis/server/api/web/application/views/basis/traceability/RequirementTextMatcherTest.java
java -cp 'web-server/target/test-classes;web-server/target/classes;common/target/classes' com.bepa.eis.server.api.web.application.views.basis.traceability.RequirementTextMatcherTest
node web-server/src/test/js/traceability-word-highlights.test.mjs
```
