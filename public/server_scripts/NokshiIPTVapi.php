<?php
/**
 * Nokshi IP TV - Official REST API & Database Bridge
 * URL: https://bcs24admin.org.bd/tv/NokshiIPTVapi.php
 *
 * File Location on Server:
 * Place this file inside: public_html/tv/NokshiIPTVapi.php
 */

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

// ==========================================
// ডেটাবেজ কনফিগারেশন (MySQL Configuration)
// ==========================================
$DB_HOST = "localhost";
$DB_NAME = "bas24db";          // আপনার ডেটাবেজ নাম
$DB_USER = "bas24db";          // আপনার ডেটাবেজ ইউজারনেম
$DB_PASS = "YOUR_DB_PASSWORD";    // আপনার ডেটাবেজ পাসওয়ার্ড

// সিক্রেট ওয়ান-টাইম পিন কোড
$SECRET_PIN = "7860"; 

try {
    $pdo = new PDO("mysql:host={$DB_HOST};dbname={$DB_NAME};charset=utf8mb4", $DB_USER, $DB_PASS, [
        PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::MYSQL_ATTR_INIT_COMMAND => "SET NAMES utf8mb4"
    ]);
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode([
        'status'  => 'error',
        'message' => 'MySQL Database Connection Failed: ' . $e->getMessage()
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

// ইনপুট গ্রহণ
$inputJSON = file_get_contents('php://input');
$body = json_decode($inputJSON, true) ?: [];

$action = $_GET['action'] ?? $body['action'] ?? 'get_channels';
$pin    = trim($_GET['pin'] ?? $body['pin'] ?? '');

switch ($action) {

    // ১. YouTube Live চ্যানেলসমূহ লোড (Category = 0, is_active = 1)
    case 'get_channels':
    case 'get_youtube_channels':
        try {
            $stmt = $pdo->prepare("
                SELECT id, ChannelURL, youtube_url, stream_name, Poster, Category, is_active, display_order 
                FROM live_streams 
                WHERE Category = 0 AND is_active = 1 
                ORDER BY display_order ASC, id ASC
            ");
            $stmt->execute();
            $records = $stmt->fetchAll();

            $channels = [];
            foreach ($records as $r) {
                $rawName = $r['stream_name'] ?? '';
                $nameParts = explode('|', $rawName);
                $cleanTitle = trim($nameParts[0]);

                $channels[] = [
                    'id'           => (int)$r['id'],
                    'channelUrl'   => $r['ChannelURL'],
                    'youtubeUrl'   => $r['youtube_url'],
                    'name'         => $cleanTitle,
                    'fullName'     => $rawName,
                    'poster'       => $r['Poster'],
                    'category'     => (int)$r['Category'],
                    'isActive'     => (bool)$r['is_active'],
                    'displayOrder' => (int)$r['display_order']
                ];
            }

            echo json_encode([
                'status'   => 'success',
                'count'    => count($channels),
                'channels' => $channels
            ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);

        } catch (PDOException $e) {
            http_response_code(500);
            echo json_encode([
                'status'  => 'error',
                'message' => 'Query error: ' . $e->getMessage()
            ], JSON_UNESCAPED_UNICODE);
        }
        break;

    // ২. ওয়ান-টাইম পিন ভেরিফিকেশন (Built-in M3U PIN verification)
    case 'verify_pin':
        if ($pin === $SECRET_PIN) {
            echo json_encode([
                'status'  => 'success',
                'valid'   => true,
                'message' => 'পিন সফলভাবে অনুমোদিত হয়েছে।',
                'm3u_url' => 'https://bcs24admin.org.bd/NokshiTV/NokshiTV.m3u'
            ], JSON_UNESCAPED_UNICODE);
        } else {
            http_response_code(401);
            echo json_encode([
                'status'  => 'error',
                'valid'   => false,
                'message' => 'ভুল পিন কোড! সঠিক পিন দিয়ে পুনরায় চেষ্টা করুন।'
            ], JSON_UNESCAPED_UNICODE);
        }
        break;

    // ৩. সার্ভার স্বাস্থ্য ও তারিখ স্থিতি
    case 'status':
        date_default_timezone_set('Asia/Dhaka');
        echo json_encode([
            'status'     => 'success',
            'server_bd_date' => date('Y-m-d'),
            'server_bd_time' => date('H:i:s'),
            'api_version'    => '1.0'
        ], JSON_UNESCAPED_UNICODE);
        break;

    default:
        echo json_encode([
            'status'  => 'error',
            'message' => 'অজানা অ্যাকশন: ' . htmlspecialchars($action)
        ], JSON_UNESCAPED_UNICODE);
        break;
}
