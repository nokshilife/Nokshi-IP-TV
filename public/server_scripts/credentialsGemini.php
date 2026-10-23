<?php
/**
 * Nokshi IP TV - Server Configuration & One-Time PIN Verification
 * URL: https://bcs24admin.org.bd/tv/credentialsGemini.php
 *
 * File Location on Server:
 * Place this file inside: public_html/tv/credentialsGemini.php
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
// ১. ডেটাবেজ ক্রেডেনশিয়ালস (MySQL Credentials)
// ==========================================
$DB_HOST = "localhost";
$DB_NAME = "bas24db";       // আপনার SQL ফাইলের ডেটাবেজ নাম
$DB_USER = "bas24db";       // আপনার cPanel MySQL ইউজারনেম বসান
$DB_PASS = "YOUR_DB_PASSWORD"; // আপনার cPanel MySQL পাসওয়ার্ড বসান

// ==========================================
// ২. বিল্ট-ইন M3U আনলক ওয়ান-টাইম পিন কোড
// ==========================================
$BUILTIN_M3U_PIN = "7860";  // এখানে আপনার পছন্দমতো গোপন পিন নির্ধারণ করুন

// রিকোয়েস্ট ডাটা রিড করা (GET / POST / JSON Body)
$inputJSON = file_get_contents('php://input');
$postData = json_decode($inputJSON, true) ?: [];

$action = $_GET['action'] ?? $postData['action'] ?? '';
$pin    = trim($_GET['pin'] ?? $postData['pin'] ?? '');

// ------------------------------------------
// এ্যাকশন ১: One-Time PIN ভেরিফিকেশন
// ------------------------------------------
if ($action === 'verify_pin' || !empty($pin)) {
    if ($pin === $BUILTIN_M3U_PIN) {
        echo json_encode([
            'status'  => 'success',
            'valid'   => true,
            'message' => 'পিন সফলভাবে যাচাই হয়েছে।',
            'm3u_url' => 'https://bcs24admin.org.bd/NokshiTV/NokshiTV.m3u'
        ], JSON_UNESCAPED_UNICODE);
    } else {
        http_response_code(401);
        echo json_encode([
            'status'  => 'error',
            'valid'   => false,
            'message' => 'ভুল পিন নম্বর! সঠিক পিন দিয়ে চেষ্টা করুন।'
        ], JSON_UNESCAPED_UNICODE);
    }
    exit;
}

// ------------------------------------------
// এ্যাকশন ২: MySQL থেকে চ্যানেল তালিকা লোড
// ------------------------------------------
if ($action === 'get_channels' || empty($action)) {
    try {
        $pdo = new PDO("mysql:host={$DB_HOST};dbname={$DB_NAME};charset=utf8mb4", $DB_USER, $DB_PASS, [
            PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        ]);

        // শুধু Category = 0 এবং is_active = 1 চ্যানেলসমূহ
        $stmt = $pdo->prepare("
            SELECT id, ChannelURL, youtube_url, stream_name, Poster, Category, is_active, display_order 
            FROM live_streams 
            WHERE Category = 0 AND is_active = 1 
            ORDER BY display_order ASC, id ASC
        ");
        $stmt->execute();
        $rows = $stmt->fetchAll();

        $channels = [];
        foreach ($rows as $row) {
            // চ্যানেল নামের টাইটেল ও বাংলা এনকোডিং ফিল্টারিং
            $rawName = $row['stream_name'] ?? '';
            // পাইপ (|) থাকলে শুধু চ্যানেলের নাম অংশ আলাদা করা
            $parts = explode('|', $rawName);
            $cleanName = trim($parts[0]);

            $channels[] = [
                'id'           => (int)$row['id'],
                'channelUrl'   => $row['ChannelURL'],
                'youtubeUrl'   => $row['youtube_url'],
                'name'         => $cleanName,
                'fullName'     => $rawName,
                'poster'       => $row['Poster'],
                'category'     => (int)$row['Category'],
                'isActive'     => (bool)$row['is_active'],
                'displayOrder' => (int)$row['display_order']
            ];
        }

        echo json_encode([
            'status'   => 'success',
            'total'    => count($channels),
            'channels' => $channels
        ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);

    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode([
            'status'  => 'error',
            'message' => 'ডেটাবেজ সংযোগ ত্রুটি: ' . $e->getMessage()
        ], JSON_UNESCAPED_UNICODE);
    }
    exit;
}
