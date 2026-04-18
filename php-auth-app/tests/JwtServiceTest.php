<?php

namespace Tests;

use App\JwtService;
use PHPUnit\Framework\TestCase;

class JwtServiceTest extends TestCase
{
    private JwtService $jwt;

    protected function setUp(): void
    {
        $this->jwt = new JwtService();
    }

    public function testEncodeReturnsNonEmptyString(): void
    {
        $user = ['id' => 1, 'email' => 'test@example.com', 'name' => 'Test User'];
        $token = $this->jwt->encode($user);

        $this->assertNotEmpty($token);
        $this->assertIsString($token);
    }

    public function testTokenHasThreeParts(): void
    {
        $user = ['id' => 1, 'email' => 'test@example.com', 'name' => 'Test User'];
        $token = $this->jwt->encode($user);

        $parts = explode('.', $token);
        $this->assertCount(3, $parts, 'JWT should have 3 dot-separated parts (header.payload.signature)');
    }

    public function testDecodeReturnsCorrectClaims(): void
    {
        $user = ['id' => 42, 'email' => 'alice@example.com', 'name' => 'Alice'];
        $token = $this->jwt->encode($user);
        $claims = $this->jwt->decode($token);

        $this->assertEquals('42', $claims['sub']);
        $this->assertEquals('alice@example.com', $claims['email']);
        $this->assertEquals('Alice', $claims['name']);
        $this->assertEquals('php-auth-app', $claims['iss']);
    }

    public function testDecodeContainsTimestampClaims(): void
    {
        $user = ['id' => 1, 'email' => 'test@example.com', 'name' => 'Test'];
        $token = $this->jwt->encode($user);
        $claims = $this->jwt->decode($token);

        $this->assertArrayHasKey('iat', $claims);
        $this->assertArrayHasKey('exp', $claims);
        $this->assertGreaterThan($claims['iat'], $claims['exp']);
        $this->assertEquals(3600, $claims['exp'] - $claims['iat'], 'Token should expire 1 hour after issue');
    }

    public function testDecodeRejectsInvalidToken(): void
    {
        $this->expectException(\Exception::class);
        $this->expectExceptionMessageMatches('/Invalid token/');
        $this->jwt->decode('invalid.token.here');
    }

    public function testDecodeRejectsTamperedToken(): void
    {
        $user = ['id' => 1, 'email' => 'test@example.com', 'name' => 'Test'];
        $token = $this->jwt->encode($user);

        // Tamper with the payload (flip a character)
        $parts = explode('.', $token);
        $parts[1] = $parts[1] . 'TAMPERED';
        $tampered = implode('.', $parts);

        $this->expectException(\Exception::class);
        $this->jwt->decode($tampered);
    }

    public function testDecodeRejectsEmptyToken(): void
    {
        $this->expectException(\Exception::class);
        $this->jwt->decode('');
    }

    public function testDifferentUsersProduceDifferentTokens(): void
    {
        $user1 = ['id' => 1, 'email' => 'a@test.com', 'name' => 'A'];
        $user2 = ['id' => 2, 'email' => 'b@test.com', 'name' => 'B'];

        $token1 = $this->jwt->encode($user1);
        $token2 = $this->jwt->encode($user2);

        $this->assertNotEquals($token1, $token2);
    }

    public function testTokenUsesRS256Algorithm(): void
    {
        $user = ['id' => 1, 'email' => 'test@example.com', 'name' => 'Test'];
        $token = $this->jwt->encode($user);

        $parts = explode('.', $token);
        $header = json_decode(base64_decode($parts[0]), true);

        $this->assertEquals('RS256', $header['alg']);
        $this->assertEquals('JWT', $header['typ']);
    }
}
