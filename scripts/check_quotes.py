"""Validate the extracted asset, including the repaired PDF page 74 boundary."""
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
quotes = json.loads((root / 'app/src/main/assets/quotes.json').read_text())
assert len(quotes) == 500
assert [q['id'] for q in quotes] == list(range(1, 501))
assert len({q['text'] for q in quotes}) == 500
assert sum(q['page'] < 23 for q in quotes) == 40
for q in quotes:
    assert 9 <= q['page'] <= q['endPage'] <= 156
    assert q['text'] and '\ufffd' not in q['text']
    assert 'YabooK' not in q['text'] and '●' not in q['text']
assert quotes[218]['text'].startswith('当你陷入困境时，睡一觉。')
assert quotes[219]['text'].startswith('无论财富、人际关系还是知识')
print('PASS: 500 unique quotes, IDs, PDF page ranges and repaired boundary')
