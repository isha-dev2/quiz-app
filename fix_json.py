import json
import os

# Read the original file
with open('app/src/main/assets/questions_clean.json', 'r') as f:
    content = f.read()

# Clean up the content
content = content.strip()
if not content.startswith('['):
    content = '[' + content
if not content.endswith(']'):
    content = content + ']'

# Parse the JSON to ensure it's valid
try:
    data = json.loads(content)
    print("JSON is valid!")
    
    # Write back the properly formatted JSON
    with open('app/src/main/assets/questions_clean.json', 'w') as f:
        json.dump(data, f, indent=2)
    print("JSON has been reformatted successfully!")
    
except json.JSONDecodeError as e:
    print(f"Error parsing JSON: {e}")
    print(f"Error at line {e.lineno}, column {e.colno}: {e.msg}")
    
    # Try to fix common JSON issues
    try:
        # Remove any trailing commas before ] or }
        import re
        fixed_content = re.sub(r',\s*([}\]])', r'\1', content)
        
        # Try parsing again
        data = json.loads(fixed_content)
        
        # Write back the fixed JSON
        with open('app/src/main/assets/questions_clean.json', 'w') as f:
            json.dump(data, f, indent=2)
        print("JSON has been fixed and reformatted!")
        
    except Exception as e2:
        print(f"Failed to fix JSON: {e2}")
        print("Please check the file for syntax errors.")
