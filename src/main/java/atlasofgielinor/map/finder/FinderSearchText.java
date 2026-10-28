/*
 * Copyright (c) 2026, n-oost
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package atlasofgielinor.map.finder;

import java.util.Locale;

/** Shared wording rules for Finder names and queries, independent of editable input. */
public final class FinderSearchText
{
	private FinderSearchText()
	{
	}

	public static String normalize(String text)
	{
		if (text == null) return "";
		final StringBuilder normalized = new StringBuilder();
		final StringBuilder word = new StringBuilder();
		final String lower = text.toLowerCase(Locale.ROOT).replace("&", " and ");
		for (int i = 0; i <= lower.length(); i++)
		{
			final char c = i < lower.length() ? lower.charAt(i) : ' ';
			if (Character.isLetterOrDigit(c)) word.append(c);
			else if (c != '\'' && c != '\u2019' && word.length() > 0)
			{
				String token = word.toString();
				if (token.length() > 4 && token.endsWith("ies"))
				{
					token = token.substring(0, token.length() - 3) + "y";
				}
				else if (token.length() > 4 && (token.endsWith("sses") || token.endsWith("ches")
					|| token.endsWith("shes") || token.endsWith("xes")))
				{
					token = token.substring(0, token.length() - 2);
				}
				else if (token.length() > 3 && token.endsWith("s")
					&& !token.endsWith("ss") && !token.endsWith("us") && !token.endsWith("is"))
				{
					token = token.substring(0, token.length() - 1);
				}
				if (normalized.length() > 0) normalized.append(' ');
				normalized.append(token);
				word.setLength(0);
			}
		}
		final String result = normalized.toString();
		return result.startsWith("the ") ? result.substring(4) : result;
	}

	public static String query(String text)
	{
		String term = normalize(text);
		if (term.startsWith("nearest ")) term = term.substring(8);
		if (term.endsWith(" near me")) term = term.substring(0, term.length() - 8);
		switch (term)
		{
			case "ge": return "grand exchange";
			case "kbd": return "king black dragon";
			case "kq": return "kalphite queen";
			case "cox": return "chamber of xeric";
			case "tob": return "theatre of blood";
			case "toa": return "tomb of amascut";
			case "gotr": return "guardian of the rift";
			case "prayer altar": return "altar";
			case "bank deposit box": return "deposit box";
			default: return term;
		}
	}

	/** Exact names, word prefixes, substrings, then reordered complete words. */
	public static int tier(String normalizedName, String term)
	{
		if (term.isEmpty() || normalizedName.isEmpty()) return -1;
		if (normalizedName.equals(term)) return 0;
		if (normalizedName.startsWith(term)
			&& normalizedName.charAt(term.length()) == ' ') return 1;
		if (normalizedName.contains(term)) return 2;
		if (term.indexOf(' ') >= 0)
		{
			final String padded = " " + normalizedName + " ";
			for (String word : term.split(" "))
			{
				if (!padded.contains(" " + word + " ")) return -1;
			}
			return 2;
		}
		return -1;
	}

	public static String mineral(String term)
	{
		for (String suffix : new String[]{" ore", " rock"})
		{
			if (term.endsWith(suffix)) return term.substring(0, term.length() - suffix.length());
		}
		return term;
	}

	/** One insertion, deletion, substitution or adjacent transposition; no edit matrix. */
	public static boolean oneEditApart(String a, String b)
	{
		if (Math.abs(a.length() - b.length()) > 1 || a.equals(b)) return false;
		int i = 0;
		while (i < a.length() && i < b.length() && a.charAt(i) == b.charAt(i)) i++;
		if (a.length() != b.length())
		{
			return a.length() > b.length() ? a.substring(i + 1).equals(b.substring(i))
				: a.substring(i).equals(b.substring(i + 1));
		}
		if (a.substring(i + 1).equals(b.substring(i + 1))) return true;
		return i + 1 < a.length() && a.charAt(i) == b.charAt(i + 1)
			&& a.charAt(i + 1) == b.charAt(i)
			&& a.substring(i + 2).equals(b.substring(i + 2));
	}
}
