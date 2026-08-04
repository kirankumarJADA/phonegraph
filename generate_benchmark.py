"""
PhoneGraph — 250-Question Benchmark Generator
================================================
Generates the evaluation benchmark described in the PhoneGraph proposal:
  - 200 core questions across 4 categories (50 each):
      1. Factual lookup
      2. Two-device comparison
      3. Recommendation with constraints
      4. Complex multi-attribute requests
  - 50 adversarial questions:
      typos, ambiguous references, out-of-distribution

Every question's ground truth is pulled DIRECTLY from the real 2,000-phone
dataset (smartphone_specs_dataset_2015_2024.csv), so every answer is
independently verifiable — this is what makes it a genuine benchmark
rather than a set of made-up test cases.

Output: benchmark_250_questions.json — used later by the Evaluation
Service (Step 7) to run PhoneGraph + all 5 baselines through the same
250 questions and score hallucination rate, P@5, MRR, nDCG@10.
"""

import pandas as pd
import random
import json

random.seed(42)

CSV_FILE = "smartphone_specs_dataset_2015_2024.csv"
df = pd.read_csv(CSV_FILE)
df = df.dropna(subset=["Phone Name"])
print(f"Loaded {len(df)} phones from dataset")

# Clean numeric columns for filtering
df["Price_num"] = pd.to_numeric(df["Price (USD)"], errors="coerce")
df["RAM_num"] = pd.to_numeric(df["RAM (GB)"], errors="coerce")
df["Storage_num"] = pd.to_numeric(df["Storage (GB)"], errors="coerce")
df["Battery_num"] = pd.to_numeric(df["Battery (mAh)"], errors="coerce")
df["Year_num"] = pd.to_numeric(df["Year Released"], errors="coerce")

records = df.to_dict("records")

questions = []
qid = 1


def add_question(category, question_text, ground_truth, relevant_phones, difficulty="medium"):
    global qid
    questions.append({
        "id": f"Q{qid:03d}",
        "category": category,
        "question": question_text,
        "ground_truth": ground_truth,
        "relevant_phones": relevant_phones,
        "difficulty": difficulty,
    })
    qid += 1


# ══════════════════════════════════════════════════════
# CATEGORY 1 — FACTUAL LOOKUP (50 questions)
# "What is the X of phone Y?" — single fact, single phone
# ══════════════════════════════════════════════════════
fact_templates = [
    ("battery capacity", "Battery (mAh)", "What is the battery capacity of the {phone}?"),
    ("RAM", "RAM (GB)", "How much RAM does the {phone} have?"),
    ("storage", "Storage (GB)", "What is the storage capacity of the {phone}?"),
    ("price", "Price (USD)", "What is the price of the {phone}?"),
    ("chipset", "Chipset", "What chipset does the {phone} use?"),
    ("main camera", "Main Camera", "What is the main camera specification of the {phone}?"),
    ("display size", "Display Size (inches)", "What is the display size of the {phone}?"),
    ("release year", "Year Released", "What year was the {phone} released?"),
    ("operating system", "OS", "What operating system does the {phone} run?"),
    ("weight", "Weight (g)", "How much does the {phone} weigh?"),
]

sample_phones_fact = random.sample(records, 50)
for i, phone in enumerate(sample_phones_fact):
    template = fact_templates[i % len(fact_templates)]
    fact_name, col, q_template = template
    answer = phone.get(col, "unknown")
    question_text = q_template.format(phone=phone["Phone Name"])
    add_question(
        "factual_lookup",
        question_text,
        f"{answer}",
        [phone["Phone Name"]],
        difficulty="easy"
    )

print(f"Generated {len([q for q in questions if q['category']=='factual_lookup'])} factual lookup questions")


# ══════════════════════════════════════════════════════
# CATEGORY 2 — TWO-DEVICE COMPARISON (50 questions)
# "Which has better X, phone A or phone B?"
# ══════════════════════════════════════════════════════
compare_attrs = [
    ("RAM_num", "RAM (GB)", "more RAM"),
    ("Battery_num", "Battery (mAh)", "a bigger battery"),
    ("Price_num", "Price (USD)", "is cheaper"),
    ("Storage_num", "Storage (GB)", "more storage"),
    ("Year_num", "Year Released", "is newer"),
]

pairs_used = set()
attempts = 0
while len([q for q in questions if q["category"] == "two_device_comparison"]) < 50 and attempts < 2000:
    attempts += 1
    p1, p2 = random.sample(records, 2)
    pair_key = tuple(sorted([p1["Phone Name"], p2["Phone Name"]]))
    if pair_key in pairs_used:
        continue
    pairs_used.add(pair_key)

    attr_col, display_col, phrase = random.choice(compare_attrs)
    v1, v2 = p1.get(attr_col), p2.get(attr_col)
    if pd.isna(v1) or pd.isna(v2) or v1 == v2:
        continue

    winner = p1["Phone Name"] if (v1 > v2) else p2["Phone Name"]
    if "cheaper" in phrase:
        winner = p1["Phone Name"] if v1 < v2 else p2["Phone Name"]
        question_text = f"Which phone {phrase}: the {p1['Phone Name']} or the {p2['Phone Name']}?"
    elif "is newer" in phrase:
        question_text = f"Which phone {phrase}: the {p1['Phone Name']} or the {p2['Phone Name']}?"
    else:
        question_text = f"Which phone has {phrase}: the {p1['Phone Name']} or the {p2['Phone Name']}?"
    ground_truth = f"{winner} ({p1[display_col] if winner==p1['Phone Name'] else p2[display_col]})"

    add_question(
        "two_device_comparison",
        question_text,
        ground_truth,
        [p1["Phone Name"], p2["Phone Name"]],
        difficulty="medium"
    )

print(f"Generated {len([q for q in questions if q['category']=='two_device_comparison'])} comparison questions")


# ══════════════════════════════════════════════════════
# CATEGORY 3 — RECOMMENDATION WITH CONSTRAINTS (50 questions)
# "Best phone under $X with Y feature"
# ══════════════════════════════════════════════════════
price_bands = [150, 200, 250, 300, 400, 500, 600, 800, 1000, 1500]
feature_constraints = [
    ("5G", "5G", "Yes"),
    ("NFC", "NFC", "Yes"),
]
brands_available = df["Brand"].dropna().unique().tolist()

rec_count = 0
attempts = 0
used_rec_questions = set()
while rec_count < 50 and attempts < 2000:
    attempts += 1
    max_price = random.choice(price_bands)
    use_brand = random.random() > 0.5
    use_feature = random.random() > 0.5

    filtered = df[df["Price_num"] <= max_price]
    q_parts = [f"under ${max_price}"]

    if use_brand:
        brand = random.choice(brands_available)
        filtered = filtered[filtered["Brand"] == brand]
        q_parts.append(f"from {brand}")

    if use_feature:
        feat_name, feat_col, feat_val = random.choice(feature_constraints)
        filtered = filtered[filtered[feat_col] == feat_val]
        q_parts.append(f"with {feat_name}")

    if len(filtered) == 0:
        continue

    question_text = f"What is the best phone {' '.join(q_parts)}?"

    # dedup check — skip if this exact question (same price/brand/feature combo) was already generated
    if question_text in used_rec_questions:
        continue
    used_rec_questions.add(question_text)

    # ground truth = cheapest matching phone with highest RAM as tiebreak (a reasonable "best" proxy)
    filtered_sorted = filtered.sort_values(["RAM_num", "Price_num"], ascending=[False, True])
    best_phone = filtered_sorted.iloc[0]

    ground_truth = f"{best_phone['Phone Name']} (${best_phone['Price (USD)']}, {best_phone['RAM (GB)']}GB RAM)"
    relevant = filtered_sorted.head(5)["Phone Name"].tolist()

    add_question(
        "recommendation_with_constraints",
        question_text,
        ground_truth,
        relevant,
        difficulty="medium"
    )
    rec_count += 1

print(f"Generated {rec_count} recommendation questions")


# ══════════════════════════════════════════════════════
# CATEGORY 4 — COMPLEX MULTI-ATTRIBUTE (50 questions)
# Multiple simultaneous constraints
# ══════════════════════════════════════════════════════
complex_count = 0
attempts = 0
used_complex_questions = set()
while complex_count < 50 and attempts < 2000:
    attempts += 1
    max_price = random.choice(price_bands)
    min_ram = random.choice([4, 6, 8, 12])
    min_battery = random.choice([4000, 4500, 5000])
    require_5g = random.random() > 0.5

    filtered = df[
        (df["Price_num"] <= max_price) &
        (df["RAM_num"] >= min_ram) &
        (df["Battery_num"] >= min_battery)
    ]
    q_parts = [f"priced under ${max_price}", f"at least {min_ram}GB RAM", f"at least {min_battery}mAh battery"]

    if require_5g:
        filtered = filtered[filtered["5G"] == "Yes"]
        q_parts.append("5G support")

    if len(filtered) == 0:
        continue

    question_text = "Find a phone " + ", ".join(q_parts[:-1]) + f", and {q_parts[-1]}."

    # dedup check — skip if this exact constraint combination was already generated
    if question_text in used_complex_questions:
        continue
    used_complex_questions.add(question_text)

    filtered_sorted = filtered.sort_values(["RAM_num", "Battery_num"], ascending=[False, False])
    best_phone = filtered_sorted.iloc[0]

    ground_truth = f"{best_phone['Phone Name']} (${best_phone['Price (USD)']}, {best_phone['RAM (GB)']}GB RAM, {best_phone['Battery (mAh)']}mAh)"
    relevant = filtered_sorted.head(5)["Phone Name"].tolist()

    add_question(
        "complex_multi_attribute",
        question_text,
        ground_truth,
        relevant,
        difficulty="hard"
    )
    complex_count += 1

print(f"Generated {complex_count} complex multi-attribute questions")


# ══════════════════════════════════════════════════════
# CATEGORY 5 — ADVERSARIAL (50 questions)
# Typos, ambiguous references, out-of-distribution
# ══════════════════════════════════════════════════════

# 5a. Typos (15 questions) — misspell a real phone name
typo_phones = random.sample(records, 15)
def make_typo(name):
    """Swap two adjacent characters in the middle of the name to simulate a typo."""
    if len(name) < 6:
        return name
    idx = random.randint(2, len(name) - 3)
    chars = list(name)
    chars[idx], chars[idx+1] = chars[idx+1], chars[idx]
    return "".join(chars)

for phone in typo_phones:
    typo_name = make_typo(phone["Phone Name"])
    question_text = f"What is the battery capacity of the {typo_name}?"
    add_question(
        "adversarial_typo",
        question_text,
        f"{phone['Battery (mAh)']} (correct phone: {phone['Phone Name']})",
        [phone["Phone Name"]],
        difficulty="hard"
    )

# 5b. Ambiguous references (15 questions) — vague, underspecified requests
ambiguous_templates = [
    "What's a good phone?",
    "Tell me about the new one.",
    "Is it worth buying?",
    "What about the camera on that phone?",
    "How does it compare to the other one?",
    "Which phone should I get?",
    "What's the best option?",
    "Is this phone good value?",
    "Tell me more about it.",
    "What's special about this one?",
    "Should I upgrade to the newer model?",
    "How good is the camera?",
    "Is the battery life good?",
    "What's the deal with this phone?",
    "Can you recommend something similar?",
]
for template in ambiguous_templates:
    add_question(
        "adversarial_ambiguous",
        template,
        "AMBIGUOUS — no specific phone referenced; correct response should ask for clarification",
        [],
        difficulty="hard"
    )

# 5c. Out-of-distribution (20 questions) — phones/brands NOT in the dataset, or nonsensical requests
ood_questions = [
    "What is the battery capacity of the iPhone 20 Pro Max?",
    "Tell me about the Samsung Galaxy Z100.",
    "What chipset does the Google Pixel 15 use?",
    "How much RAM does the Nokia 3350 have?",
    "What is the price of the OnePlus 20?",
    "Tell me about the Huawei P100.",
    "What camera does the Xiaomi 20 Ultra have?",
    "How much does the Sony Xperia 3 VII cost?",
    "What is the best phone with a foldable holographic display?",
    "Which phone has a 1TB battery?",
    "What phone has negative price?",
    "Tell me about the Motorola Razr 3000.",
    "What is the storage capacity of the Apple Vision Phone?",
    "How much RAM does the Nothing Phone 5 have?",
    "What is the release year of the Samsung Galaxy S99?",
    "Tell me about phones that don't have a battery.",
    "What is the weight of a phone made of antimatter?",
    "Which phone costs exactly $0?",
    "What phone has 500GB of RAM?",
    "Tell me about the Vivo X1000.",
]
for q in ood_questions:
    add_question(
        "adversarial_ood",
        q,
        "OUT-OF-DISTRIBUTION — phone/spec does not exist in the knowledge graph; correct response should state this rather than inventing an answer",
        [],
        difficulty="hard"
    )

print(f"Generated {len([q for q in questions if q['category'].startswith('adversarial')])} adversarial questions")


# ══════════════════════════════════════════════════════
# TRIM TO EXACT COUNTS AND SAVE
# ══════════════════════════════════════════════════════
by_category = {}
for q in questions:
    by_category.setdefault(q["category"], []).append(q)

target_counts = {
    "factual_lookup": 50,
    "two_device_comparison": 50,
    "recommendation_with_constraints": 50,
    "complex_multi_attribute": 50,
    "adversarial_typo": 15,
    "adversarial_ambiguous": 15,
    "adversarial_ood": 20,
}

final_questions = []
for cat, count in target_counts.items():
    final_questions.extend(by_category.get(cat, [])[:count])

# renumber IDs cleanly
for i, q in enumerate(final_questions, 1):
    q["id"] = f"Q{i:03d}"

print(f"\nFINAL TOTAL: {len(final_questions)} questions")
for cat, count in target_counts.items():
    actual = len([q for q in final_questions if q["category"] == cat])
    print(f"  {cat}: {actual}/{count}")

output = {
    "benchmark_name": "PhoneGraph Evaluation Benchmark",
    "total_questions": len(final_questions),
    "categories": target_counts,
    "source_dataset": CSV_FILE,
    "source_phone_count": len(df),
    "questions": final_questions
}

with open("/mnt/user-data/outputs/benchmark_250_questions.json", "w") as f:
    json.dump(output, f, indent=2)

print("\nSaved to benchmark_250_questions.json")
