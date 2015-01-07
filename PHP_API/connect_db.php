<?php

require_once __DIR__ . '/db_config.php';

/**
 * Function to connect with database
 */
function connect_db() {

    $connection = new PDO("mysql:host=".HOSTNAME.";dbname=".DB_NAME, DB_USERNAME, DB_PASSWORD);
    $connection->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    $stmt = $connection->prepare("SET NAMES utf8");

    $stmt->execute();

    return $connection;
}


?>