<?php

/*
 * Returns all Store Elements with the Id 127
 *
 * Do NOT replace the old get_toilets.php with this file!
 * Old versions of the app will stop working.
 */

$response = array();
$response["success"] = 0;

require_once __DIR__ . '/connect_db.php';
require_once __DIR__ . '/helpers.php';

$cat_id = 127;

$headers = getallheaders();

if ($headers["Authkey"] == AUTH_KEY) {
    try {
        $dbh = connect_db();
        $dbh->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

        $query = "SELECT * FROM stores WHERE cat_id = :cat_id AND status = 1 AND approved = 1";

        $stmt = $dbh->prepare($query);

        $stmt->execute(array('cat_id' => $cat_id));
     
        $result = $stmt->fetchAll();

        if ( count($result) ) {
            // success
            $data = array();

            foreach($result as $row) {
                $store = array();
                $store["id"] = $row["id"];
                $store["name"] = $row["name"];
                $store["address"] = $row["address"];
                $store["website"] = $row["website"];
                $store["description"] = $row["description"];
                $store["latitude"] = $row["latitude"];
                $store["longitude"] = $row["longitude"];

                array_push($data, $store);
            }
            $response["success"] = 1;
            $response["data"] = $data;

        } else {
            $response["message"] = "Es konnten keine Elemente gefunden werden.";
            $response["error"] = "Querry result is empty.";
        }
    } catch(PDOException $e) {
        $response["message"] = "Es konnten keine Elemente gefunden werden.";
        $response["error"] = $e->getMessage();
    }
}

echo json_encode($response);

?>