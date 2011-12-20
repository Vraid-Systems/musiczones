<?php
header("Access-Control-Allow-Origin: *"); //http://enable-cors.org/#how-php

/**
 * SQLite database and table setup
 */
$myDatabaseFile = 'mz.db';
$myDbExistsBoolean = file_exists($myDatabaseFile);
$SQLite_conn = new SQLiteDatabase($myDatabaseFile);
if (!$myDbExistsBoolean) { //db did not exist before "new" call
    $aSqlCreateNodeTable = "CREATE TABLE Nodes "
            . "(id INTEGER PRIMARY KEY ASC, uuid TEXT, name TEXT, "
            . "wanaddress TEXT, localaddress TEXT, http INTEGER, online INTEGER)";
    $aSqlBoolRet = $SQLite_conn->queryExec($aSqlCreateNodeTable, $error);
    if (!$aSqlBoolRet) {
        die($error);
    }
}

/**
 * the main logic
 */
$myEOLMarker = "\nEOL\n";
$mySplitStr = "::";
if (isset($_GET['opt']) && ($_GET['opt'] != '')) {
    $opt = $_GET['opt'];
    if ($opt == 'address') {
        echo 'address=' . $_SERVER["REMOTE_ADDR"] . $myEOLMarker;
    } elseif ($opt == 'online') {
        $address = $_SERVER["REMOTE_ADDR"];
        if (isset($_GET['address']) && ($_GET['address'] != '')) {
            $address = $_GET['address'];
        }
        $aReturnArray = getNodesOnline($SQLite_conn, $address);
        if ($aReturnArray) {
            for ($i = 0; $i < sizeof($aReturnArray); $i++) {
                echo $aReturnArray[$i]['name']
                . $mySplitStr . $aReturnArray[$i]['address']
                . $mySplitStr . $aReturnArray[$i]['http'] . $myEOLMarker;
            }
        }
    } elseif ($opt == 'ping') {
        if (isset($_GET['remove']) && ($_GET['remove'] != '')) {
            setNodeIsOffline($SQLite_conn, $_GET['uuid']);
        } else {
            setNodeIsOnline($SQLite_conn, $_GET['uuid'], $_GET['name'], $_SERVER["REMOTE_ADDR"], $_GET['address'], $_GET['http']);
        }
    } else {
        echo "ERROR - invalid opt parameter\n";
    }
} else {
    $aLocalHttpUrl = 'http://' . $_SERVER['SERVER_NAME']
            . $_SERVER['SCRIPT_NAME'] . '?opt=online&address=' . $_SERVER["REMOTE_ADDR"];
    $aRawZoneList = file_get_contents($aLocalHttpUrl);
    $aZoneLines = explode($myEOLMarker, $aRawZoneList);
    if ((substr_count($aRawZoneList, $myEOLMarker) == 0)
            || (sizeof($aZoneLines) < 1) || ($aZoneLines[0] == "")) {
        $aZoneLines = false;
    }
    ?>
    <!DOCTYPE html>
    <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1" />

            <link rel="shortcut icon" href="/favicon.ico" />

            <title>MusicZones</title>
            <!-- <?php echo $aLocalHttpUrl; ?> -->
            <meta name="robots" content="noindex,nofollow" />

            <link rel="stylesheet" href="http://code.jquery.com/mobile/1.0/jquery.mobile-1.0.min.css" />
            <script src="http://code.jquery.com/jquery-1.6.4.min.js"></script>
            <script src="http://code.jquery.com/mobile/1.0/jquery.mobile-1.0.min.js"></script>
        </head>
        <body>
            <div id="zoneController" data-role="page" data-theme="d">
                <div data-role="header" data-theme="b" data-position="fixed">
                    <h1>MusicZones</h1>
                </div>

                <div id="zoneContent" data-role="content">
                    <?php
                    if ($aZoneLines) {
                        ?>
                        <ul data-role="listview" data-inset="true" data-filter="true">
                            <?php
                            foreach ($aZoneLines as $aZoneLine) {
                                $aZoneLineArray = explode($mySplitStr, $aZoneLine);
                                if (sizeof($aZoneLineArray) == 3) {
                                    //[name]$mySplitStr[address]$mySplitStr[http]
                                    echo '<li><a href="http://' . $aZoneLineArray[1]
                                    . ':' . $aZoneLineArray[2] . '">'
                                    . $aZoneLineArray[0] . '</a></li>';
                                }
                            }
                            ?>
                        </ul>
                        <?php
                    } else {
                        ?>
                        <p>
                            You do not seem to have any
                            <a href="http://vraidsys.com/musiczones/">MusicZones</a>
                            online. Why not start a few?
                        </p>
                        <?php
                    }
                    ?>
                </div>
            </div>
        </body>
    </html>
    <?php
}

/**
 * return a 2-d array containing node information for all available nodes
 * @param SQLite $SQLite_conn
 * @param string $theWanAddress - the WAN IP Address to match on
 * @return array
 */
function getNodesOnline($SQLite_conn, $theWanAddress) {
    if (!filter_var($theWanAddress, FILTER_VALIDATE_IP)) {
        return false; //http://www.w3schools.com/php/filter_validate_ip.asp
    }

    $results = $SQLite_conn->query("SELECT name, localaddress, http "
            . "FROM Nodes WHERE online=1 AND wanaddress='$theWanAddress'");
    if (!$results) {
        return false;
    }

    $i = 0;
    while ($row = $results->fetch()) {
        $aReturnArray[$i]['name'] = $row['name'];
        $aReturnArray[$i]['address'] = $row['localaddress'];
        $aReturnArray[$i]['http'] = $row['http'];
        $i++;
    }

    return $aReturnArray;
}

/**
 * set that a certain node is online with certain params
 * @param SQLite $SQLite_conn
 * @param string $theUUID
 * @param string $theNodeName
 * @param string $theWanAddress
 * @param string $theLocalAddress
 * @param int $theWebPort
 * @return boolean - did the set work?
 */
function setNodeIsOnline($SQLite_conn, $theUUID, $theNodeName, $theWanAddress, $theLocalAddress, $theWebPort) {
    $theUUID = filterSqlVariable($theUUID);
    $theNodeName = filterSqlVariable($theNodeName);
    if (!filter_var($theWanAddress, FILTER_VALIDATE_IP)
            || !filter_var($theLocalAddress, FILTER_VALIDATE_IP)) {
        return false; //http://www.w3schools.com/php/filter_validate_ip.asp
    }
    $theWebPort = filterSqlVariable($theWebPort);

    $aResultCount = $SQLite_conn->singleQuery("SELECT COUNT(*) FROM Nodes "
            . "WHERE uuid='$theUUID'");
    if ($aResultCount == 0) {
        return ($SQLite_conn->queryExec("INSERT INTO Nodes VALUES(NULL, '$theUUID', "
                        . "'$theNodeName', '$theWanAddress', '$theLocalAddress', "
                        . "'$theWebPort', 1)"));
    } elseif ($aResultCount == 1) {
        return ($SQLite_conn->queryExec("UPDATE Nodes SET wanaddress='$theWanAddress',"
                        . " localaddress='$theLocalAddress', http='$theWebPort',"
                        . " online=1 WHERE uuid='$theUUID'"));
    } else {
        return false;
    }
}

/**
 * set that a certain node with a certain UUID is offline
 * @param SQLite $SQLite_conn
 * @param string $theUUID
 * @return boolean - did everything work properly?
 */
function setNodeIsOffline($SQLite_conn, $theUUID) {
    $theUUID = filterSqlVariable($theUUID);

    $aResultCount = $SQLite_conn->singleQuery("SELECT COUNT(*) FROM Nodes "
            . "WHERE uuid='$theUUID'");
    if ($aResultCount == 1) {
        return ($SQLite_conn->queryExec("UPDATE Nodes SET online=0 WHERE uuid='$theUUID'"));
    } else {
        return false;
    }
}

/**
 * minimal SQL filtering function to foil stupid attackers
 * only allows periods, dashes, underscores, and alphanumeric characters
 */
function filterSqlVariable($theDataVariable) {
    return (ereg_replace("[^A-Za-z0-9.-_]", "", $theDataVariable));
}
?>
