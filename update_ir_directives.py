#!/usr/bin/env python3
"""
Script to update @IR directives in EK9 test files based on test output.
Parses structured output from IRDirectiveListener and updates source files.

Usage:
    python3 update_ir_directives.py <test_output_file>
    cat test_output.txt | python3 update_ir_directives.py -
"""

import re
import sys
import os

def parse_ir_blocks(input_text):
    """Parse structured IR update blocks from test output."""
    blocks = []
    pattern = r'===IR_UPDATE_START===\s*\n' \
              r'FILE:\s*(.+?)\s*\n' \
              r'SYMBOL:\s*(.+?)\s*\n' \
              r'CATEGORY:\s*(.+?)\s*\n' \
              r'---IR_CONTENT_START---\s*\n' \
              r'(.*?)' \
              r'---IR_CONTENT_END---\s*\n' \
              r'===IR_UPDATE_END==='

    matches = re.findall(pattern, input_text, re.DOTALL)
    for match in matches:
        file_path, symbol, category, ir_content = match
        blocks.append({
            'file': file_path.strip(),
            'symbol': symbol.strip(),
            'category': category.strip(),
            'ir': ir_content.rstrip('\n')
        })
    return blocks

def convert_target_to_source_path(target_path):
    """Convert target/test-classes path to src/test/resources path."""
    if '/target/test-classes/' in target_path:
        return target_path.replace('/target/test-classes/', '/src/test/resources/')
    return target_path

def update_ir_directive(file_path, symbol, ir_content):
    """Update the @IR directive in the source file."""
    if not os.path.exists(file_path):
        print(f"WARNING: File not found: {file_path}")
        return False

    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Pattern to match @IR directive for this symbol
    # Format: @IR: PHASE: CATEGORY: "symbol": `content`
    escaped_symbol = re.escape(symbol)
    pattern = r'(@IR:\s*\w+:\s*\w+:\s*"' + escaped_symbol + r'":\s*`)([^`]*?)(`)'

    def replacement(match):
        prefix = match.group(1)
        suffix = match.group(3)
        return prefix + ir_content + suffix

    new_content, count = re.subn(pattern, replacement, content, flags=re.DOTALL)

    if count == 0:
        print(f"WARNING: Could not find @IR directive for symbol '{symbol}' in {file_path}")
        return False

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(new_content)

    print(f"Updated: {file_path} (symbol: {symbol})")
    return True

def main():
    if len(sys.argv) < 2:
        print("Usage: python3 update_ir_directives.py <test_output_file>")
        print("       cat test_output.txt | python3 update_ir_directives.py -")
        sys.exit(1)

    input_file = sys.argv[1]
    if input_file == '-':
        input_text = sys.stdin.read()
    else:
        with open(input_file, 'r', encoding='utf-8') as f:
            input_text = f.read()

    blocks = parse_ir_blocks(input_text)
    print(f"Found {len(blocks)} IR update blocks")

    updated, failed = 0, 0
    seen = set()

    for block in blocks:
        key = (block['file'], block['symbol'])
        if key in seen:
            continue
        seen.add(key)

        source_path = convert_target_to_source_path(block['file'])
        if update_ir_directive(source_path, block['symbol'], block['ir']):
            updated += 1
        else:
            failed += 1

    print(f"\nSummary: {updated} files updated, {failed} failed")

if __name__ == '__main__':
    main()
