#!/usr/bin/env node

const { spawn } = require('child_process');

async function testValidEk9() {
  console.log('=== Testing Valid EK9 Code via MCP ===\n');

  const validCode = `#!ek9
<?-
  Valid EK9 program demonstrating correct syntax.
  Uses proper assignment operators and EK9 constructs.
-?>
defines module net.customer.mcp.test.valid

  defines function

    testValid()
      stdout <- Stdout()
      value <- String("Hello EK9!")
      stdout.println(\`Message: \${value}\`)
      
      // Test tri-state semantics
      unsetValue <- String()
      stdout.println(\`Unset value isSet: \${unsetValue?}\`)

  defines program

    main()
      testValid()

//EOF`;

  const command = {
    jsonrpc: "2.0",
    method: "tools/call",
    params: {
      name: "validate_ek9_syntax",
      arguments: {
        code: validCode,
        filename: "test_valid.ek9"
      }
    },
    id: 1
  };

  console.log('Testing valid EK9 code:');
  console.log(validCode);
  console.log('\n--- MCP Validation Result ---');

  const result = await sendMcpCommand(command);
  
  if (result.result && result.result.content && result.result.content[0]) {
    console.log(result.result.content[0].text);
  } else {
    console.log('Full result:', JSON.stringify(result, null, 2));
  }
}

async function sendMcpCommand(command) {
  return new Promise((resolve, reject) => {
    const mcpProcess = spawn('node', ['standalone-mcp-ek9.js'], {
      stdio: ['pipe', 'pipe', 'pipe']
    });

    let output = '';
    let error = '';

    mcpProcess.stdout.on('data', (data) => {
      output += data.toString();
    });

    mcpProcess.stderr.on('data', (data) => {
      error += data.toString();
    });

    mcpProcess.on('close', (code) => {
      try {
        const lines = output.split('\n').filter(line => line.trim());
        for (const line of lines) {
          try {
            const response = JSON.parse(line);
            if (response.jsonrpc && response.id) {
              resolve(response);
              return;
            }
          } catch (e) {
            // Continue searching for JSON response
          }
        }
        reject(new Error(`No valid response found`));
      } catch (parseError) {
        reject(parseError);
      }
    });

    mcpProcess.stdin.write(JSON.stringify(command) + '\n');
    mcpProcess.stdin.end();
  });
}

testValidEk9()
  .then(() => console.log('\n=== Valid code test completed ==='))
  .catch(error => {
    console.error('Test failed:', error);
    process.exit(1);
  });