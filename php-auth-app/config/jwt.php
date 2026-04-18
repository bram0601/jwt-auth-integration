<?php

return [
    'private_key_path' => getenv('JWT_PRIVATE_KEY') ?: __DIR__ . '/../../keys/private.pem',
    'public_key_path'  => getenv('JWT_PUBLIC_KEY') ?: __DIR__ . '/../../keys/public.pem',
    'algorithm'        => 'RS256',
    'issuer'           => 'php-auth-app',
    'expiry_seconds'   => 3600, // 1 hour
];
