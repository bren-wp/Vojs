<?php
declare(strict_types=1);
// No scheduled jobs or legacy audio storage are used by the P2P-only VOJS backend.
if (PHP_SAPI !== 'cli') { http_response_code(404); exit; }
echo "VOJS P2P does not require cron; there are no recordings to clean.\n";
