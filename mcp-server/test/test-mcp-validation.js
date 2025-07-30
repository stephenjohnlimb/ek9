#!/usr/bin/env node

/**
 * Test script for MCP-EK9 validation
 */

const { spawn } = require('child_process');
const fs = require('fs');

async function testMcpValidation() {
  console.log('=== Testing MCP-EK9 Validation ===\n');

  // Test 1: JAR Status
  console.log('1. Testing JAR Status Check...');
  await testJarStatus();

  // Test 2: Valid EK9 Code
  console.log('\n2. Testing Valid EK9 Code...');
  await testValidEk9();

  // Test 3: Invalid EK9 Code
  console.log('\n3. Testing Invalid EK9 Code...');
  await testInvalidEk9();
}

async function testJarStatus() {
  const command = {
    jsonrpc: "2.0",
    method: "tools/call",
    params: {
      name: "check_ek9_jar_status",
      arguments: {}
    },
    id: 1
  };

  const result = await sendMcpCommand(command);
  console.log('JAR Status Result:', result);
}

async function testValidEk9() {
  const validCode = `#!ek9
<?-
  Valid EK9 program for MCP testing.
  Uses proper assignment operators and syntax.
-?>
defines module net.customer.mcp.test

  defines function

    testValid()
      stdout <- Stdout()
      value <- String("Hello EK9!")
      stdout.println(\`Value: \${value}\`)

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
    id: 2
  };

  const result = await sendMcpCommand(command);
  console.log('Valid Code Result:', result);
}

async function testInvalidEk9() {
  const invalidCode = `#!ek9
<?-
  Invalid EK9 program for MCP testing.
  Uses incorrect assignment operator (= instead of <-).
-?>
defines module net.customer.mcp.test.bad

  defines function

    testInvalid()
      stdout <- Stdout()
      badValue = String("This should fail")
      stdout.println(\`Value: \${badValue}\`)

  defines program

    main()
      testInvalid()

//EOF`;

  const command = {
    jsonrpc: "2.0",
    method: "tools/call",
    params: {
      name: "validate_ek9_syntax",
      arguments: {
        code: invalidCode,
        filename: "test_invalid.ek9"
      }
    },
    id: 3
  };

  const result = await sendMcpCommand(command);
  console.log('Invalid Code Result:', result);
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
        // Parse the JSON response from stdout
        const lines = output.split('\n').filter(line => line.trim());
        for (const line of lines) {
          try {
            const response = JSON.parse(line);
            if (response.jsonrpc && response.id) {
              resolve(response);
              return;
            }
          } catch (e) {
            // Not a JSON response, continue
          }
        }
        reject(new Error(`No valid JSON response found. Output: ${output}, Error: ${error}`));
      } catch (parseError) {
        reject(new Error(`Failed to parse response: ${parseError.message}`));
      }
    });

    mcpProcess.on('error', (err) => {
      reject(err);
    });

    // Send the command
    mcpProcess.stdin.write(JSON.stringify(command) + '\n');
    mcpProcess.stdin.end();
  });
}

// Run tests
if (require.main === module) {
  testMcpValidation()
    .then(() => {
      console.log('\n=== All MCP-EK9 tests completed ===');
    })
    .catch((error) => {
      console.error('Test failed:', error);
      process.exit(1);
    });
}