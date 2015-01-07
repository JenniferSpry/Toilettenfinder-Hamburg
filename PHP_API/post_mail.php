<?php

  $response = array();
  $response["success"] = 0;

  require_once __DIR__ . '/helpers.php';
  require_once __DIR__ . '/db_config.php';

  $headers = getallheaders();

  if ($headers["Authkey"] == AUTH_KEY) {
    $inputJSON = file_get_contents('php://input');
    $input = json_decode( $inputJSON, TRUE ); //convert JSON into array
    if (isset($input["content"])) {
    
      $mailAddress = $input["emailaddress"];
      $name = $input["name"];

      $content = $input["content"];
      $content = $content . "\n\n" . 
        "Gesendet von: " . $name . " (" . $mailAddress . ")" . "\n\n" .
        "Toilette: ". $input["poiName"] . ", " . $input["poiAddress"] . " (Id: " . $input["poiId"] . ")". "\n\n" .
        "User-Agent: ". $headers["User-Agent"] ;

      $subject = 'Toilettenfinder App Formular';

      if ($content != '') {
        if (mail (MAIL_RECIEVER, $subject, $content)) {
          $response["success"] = 1;
        }
      }
    }
  }
  echo json_encode($response); 
?>