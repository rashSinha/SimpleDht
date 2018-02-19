package edu.buffalo.cse.cse486586.simpledht;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

import static android.content.Context.MODE_MULTI_PROCESS;


public class SimpleDhtProvider extends ContentProvider {

    public static final String PREFS_NAME = "MyPrefsFile";
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";

    static final int SERVER_PORT = 10000;
    static final String TAG = SimpleDhtProvider.class.getSimpleName();

    //private static int seq = 0;
    private String remotePort[] = {"11108", "11112", "11116", "11120", "11124"};
    private String fPort = "11108";
    private String portStr = "";
    private String myPort = "";
    private String emulatorId[] = {"5554", "5556", "5558", "5560", "5562"};
    private final Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider");
    private static final String fNode = "5554";
    private static String fNodeHashed ;

    private static ArrayList aList = new ArrayList();
    private static ArrayList arrays = new ArrayList();
    private static ArrayList<String> arrayList = new ArrayList<String>();
    private static HashMap hashMap = new HashMap();
    private static HashMap<String, String> hashed = new HashMap<String, String>();
    String routedQuery = "";
    String queryString = "";
    String [] querySelection;
    ArrayList array = new ArrayList();
    private static MatrixCursor cursor;

    private static String nodeId;
    private static String predecessorNode = null;
    private static String successorNode = null;
    String predecessorPort;
    String successorPort;
    String successor;
    String predecessor;
    String avdId;
    private static String msgId;
    private static String currentNode;
    private static String currentNodeHashed;


    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
            Log.i(TAG, "delete delete delete delete");

        SharedPreferences sharedPrefs = this.getContext().getSharedPreferences(PREFS_NAME, 4);
        if(sharedPrefs.contains(selection))
        {
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.remove(selection);
            editor.commit();
            array.remove(selection);
        }
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub

        SharedPreferences sharedPreferences = getContext().getSharedPreferences(PREFS_NAME, 4);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Log.i(TAG, String.valueOf(editor));

        arrayList.add(values.getAsString(KEY_FIELD));

        Log.i(TAG, "values of key and value:" + values.getAsString(KEY_FIELD) + " " + values.getAsString(VALUE_FIELD));
        String message = values.getAsString(KEY_FIELD) + "," + values.getAsString(VALUE_FIELD);

        //editor.commit();
        currentNode = portStr;
        try {
            currentNodeHashed = genHash(portStr);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            msgId = genHash(values.getAsString(KEY_FIELD));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        Log.i(TAG, predecessorNode + " " + successorNode);

        Log.i(TAG, "Size of arrays");
        Log.i(TAG,arrays.toString());
        if((arrays.size() == 1))
        {
            Log.i(TAG, "editor contents when only one node is present");
            predecessorNode = currentNodeHashed;
            Log.i(TAG, predecessorNode);
            successorNode = currentNodeHashed;
            Log.i(TAG, successorNode);
            editor.putString(values.getAsString(KEY_FIELD), values.getAsString(VALUE_FIELD));
            array.add(KEY_FIELD);
            editor.commit();
        }
        else {
            try {
                predecessorNode=genHash(predecessorPort);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            try {
                successorNode=genHash(successorPort);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            if((msgId.compareTo(predecessorNode) > 0) && (msgId.compareTo(currentNodeHashed) <= 0)) {
                Log.i(TAG, "editor contents when msgId is greater than predNode");
                Log.i(TAG,"Inside condition 1");
                editor.putString(values.getAsString(KEY_FIELD), values.getAsString(VALUE_FIELD));
                array.add(KEY_FIELD);
                editor.commit();
            }
            else if ((predecessorNode.compareTo(currentNodeHashed) > 0) && ((msgId.compareTo(predecessorNode) > 0) || (msgId.compareTo(currentNodeHashed) <= 0))) {
                Log.i(TAG,"Inside condition 2");
                Log.i(TAG, "editor contents when its the first node");
                editor.putString(values.getAsString(KEY_FIELD), values.getAsString(VALUE_FIELD));
                array.add(KEY_FIELD);
                editor.commit();
            }
            else{
                Log.i(TAG,"Inside condition 3");
                Log.i(TAG,"Sending request to successor for key " + message);
                Log.i(TAG, "generate new client task since this is the last loop");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "insert", String.valueOf(Integer.parseInt(currentNode) * 2), String.valueOf(Integer.parseInt(successorPort) * 2), message);
//                        Thread.sleep(400);
            }
        }

        Log.v("insert", values.toString());
        return uri;

        //return null;
    }

    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub

        TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));
            try {
                nodeId = genHash(portStr);
                Log.i(TAG, nodeId);
            } catch (NoSuchAlgorithmException e) {
                Log.e(TAG, "Hash not generated");
                e.printStackTrace();
            }

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            //return;
        }

        try {
            fNodeHashed = genHash(fNode);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "node generated" +fNodeHashed);
            e.printStackTrace();
        }

        if(nodeId.equals(fNodeHashed)) {
            hashed.put(fNodeHashed,fNode);
            Log.i(TAG, "added to hashmap" +hashed);
            aList.add(fNodeHashed);
            arrays.add(fNodeHashed);
            Log.i(TAG, "added to arrayList" + arrays);

            predecessorNode = nodeId;
            successorNode = nodeId;

            Log.i(TAG, "pred and succ are same" +nodeId);
        }
        else {
            arrays.add(nodeId);
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "new node join" + portStr, myPort);
        }
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        // TODO Auto-generated method stub

        //matrixCursor.addRow(new String[] {selection, sharedPreferences.getString(selection,"")});

        Log.i(TAG, "Checking selection" + selection);
//        String sourcePort = "";
//
        selection = selection.trim();
//        //String message = String.valueOf(query(mUri, null, selection, null, null));
//        if (selection.contains(",")) {
//            querySelection = selection.split(",");
//            sourcePort = querySelection[1];
//            selection = querySelection[0];
//        }


        Log.i(TAG, String.valueOf(arrays.size()));
        //if (arrays.size() == 1) {
            Log.i(TAG, "only one node in the system");
            if (selection.equals("*")) {
                Log.i(TAG, "Printing keys top - * query");

                MatrixCursor matrixCursor = new MatrixCursor(new String[]{KEY_FIELD, VALUE_FIELD});
                Map<String, ?> allEntries = this.getContext().getSharedPreferences(PREFS_NAME, MODE_MULTI_PROCESS).getAll();
                for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                    Log.i(TAG, entry.getKey() + ": " + entry.getValue().toString());
                    matrixCursor.addRow(new Object[]{entry.getKey(), entry.getValue().toString()});
                }
                return matrixCursor;
            }

            else if (selection.equals("@")) {
                Log.i(TAG, "Printing keys top - @ query");
                MatrixCursor matrixCursor = new MatrixCursor(new String[]{KEY_FIELD, VALUE_FIELD});
                Map<String, ?> allEntries = this.getContext().getSharedPreferences(PREFS_NAME, MODE_MULTI_PROCESS).getAll();
                for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                    Log.i(TAG, entry.getKey() + ": " + entry.getValue().toString());
                    matrixCursor.addRow(new Object[]{entry.getKey(), entry.getValue().toString()});
                }
                return matrixCursor;
            } else {
                Log.i(TAG, "Else");
                MatrixCursor matrixCursor = new MatrixCursor(new String[]{KEY_FIELD, VALUE_FIELD});
                matrixCursor.addRow(new String[]{selection, this.getContext().getSharedPreferences(PREFS_NAME, MODE_MULTI_PROCESS).getString(selection, "")});
                hashMap.get(KEY_FIELD);
                Log.i(TAG, String.valueOf(hashMap));
                return matrixCursor;
            }
//        }
//        else {
//            if (selection.charAt(1) == '@') {
//                Log.i(TAG, "Printing keys top - @ query");
//                MatrixCursor matrixCursor = new MatrixCursor(new String[]{KEY_FIELD, VALUE_FIELD});
//                Map<String, ?> allEntries = this.getContext().getSharedPreferences(PREFS_NAME, MODE_MULTI_PROCESS).getAll();

//                if (sourcePort != null && sourcePort.equals("")) {
//                    String str = "";
//                    for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
//                        Log.i(TAG, entry.getKey() + ": " + entry.getValue().toString());
//                        str = String.valueOf(new Object[]{entry.getKey() + "," + entry.getValue().toString()+ "-"});
//                        str = str.substring(0, str.length()-1);
//                        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "resQue" + "@" + sourcePort + "@" , str);
//                    }
//                }
//                else {
//                    for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
//                        Log.i(TAG, entry.getKey() + ": " + entry.getValue().toString());
//                        matrixCursor.addRow(new Object[]{entry.getKey(), entry.getValue().toString()});
//                    }
//                }
//                return matrixCursor;
//            }
//            else if (selection.charAt(1) == '*') {
//                MatrixCursor matrixCursor = new MatrixCursor(new String[]{KEY_FIELD, VALUE_FIELD});
//                    for(int i =0; i<=5; i++) {
//                        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "star", remotePort[i]);
//                    }
//                try {
//                    Thread.sleep(3000);
//                } catch (InterruptedException e) {
//                    Log.i(TAG,"thread slept");
//                    e.printStackTrace();
//                }
//                String receivedQuery = queryString;
//                        receivedQuery = receivedQuery.substring(0, receivedQuery.length()-1);
//                        queryString = "";
//                        String[] str = receivedQuery.split("-");
//                        for(int j = 0; j<str.length; j++)
//                        {
//                            matrixCursor.addRow(new Object[]{str[j].split(",")[0],str[j].split(",")[1]});
//                        }
//                return matrixCursor;
//            }
//            else {
//                currentNode = portStr;
//                try {
//                    currentNodeHashed = genHash(portStr);
//                } catch (NoSuchAlgorithmException e) {
//                    e.printStackTrace();
//                }
//                try {
//                    msgId = genHash(selection);
//                } catch (NoSuchAlgorithmException e) {
//                    e.printStackTrace();
//                }
//                try {
//                    predecessorNode = genHash(predecessorPort);
//                } catch (NoSuchAlgorithmException e) {
//                    e.printStackTrace();
//                }
//                try {
//                    successorNode = genHash(successorPort);
//                } catch (NoSuchAlgorithmException e) {
//                    e.printStackTrace();
//                }
//
//                if ((msgId.compareTo(predecessorNode) > 0) && (msgId.compareTo(currentNodeHashed) <= 0)) {
//                    Log.i(TAG, "query contents when msgId is greater than predNode");
//                    Log.i(TAG, "Inside condition 1");
//                    MatrixCursor matrixCursor = new MatrixCursor(new String[]{KEY_FIELD, VALUE_FIELD});
//                    if (sourcePort != null && sourcePort.equals(" ")) {
//                        String msg = String.valueOf(new String[]{selection, this.getContext().getSharedPreferences(PREFS_NAME, MODE_MULTI_PROCESS).getString(selection, "")});
//                        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "result" + "," + sourcePort + "," , msg);
//                    } else {
//                        matrixCursor.addRow(new String[]{selection, this.getContext().getSharedPreferences(PREFS_NAME, MODE_MULTI_PROCESS).getString(selection, "")});
//                        return matrixCursor;
//                    }
//                } else if ((predecessorNode.compareTo(currentNodeHashed) > 0) && ((msgId.compareTo(predecessorNode) > 0) || (msgId.compareTo(currentNodeHashed) <= 0))) {
//                    Log.i(TAG, "Inside condition 2");
//                    Log.i(TAG, "query contents when its the first node");
//                    MatrixCursor matrixCursor = new MatrixCursor(new String[]{KEY_FIELD, VALUE_FIELD});
//                    matrixCursor.addRow(new String[]{selection, this.getContext().getSharedPreferences(PREFS_NAME, MODE_MULTI_PROCESS).getString(selection, "")});
//                    return matrixCursor;
//                } else {
//                    Log.i(TAG, "Else");
//                    MatrixCursor matrixCursor = new MatrixCursor(new String[]{KEY_FIELD, VALUE_FIELD});
//                    hashMap.get(KEY_FIELD);
//                    Log.i(TAG, String.valueOf(hashMap));
//                    Log.i(TAG, "current node:" + currentNode);
//                    Log.i(TAG, "succ port is:"+ successorPort);
//                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "query" + "," + String.valueOf(Integer.parseInt(currentNode) * 2) + "," + String.valueOf(Integer.parseInt(successorPort) * 2) + "," , querySelection[0]);
//                    try {
//                        Thread.sleep(4000);
//                    } catch (InterruptedException e) {
//                        Log.i(TAG, "thread interrupted");
//                        e.printStackTrace();
//                    }
//                    matrixCursor.addRow(new String[]{selection, routedQuery});
//                    return matrixCursor;
//                }
//            }
//        }
//
//        return null;
    }


    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    public void updateNodes(){
        String update = avdId+","+successor+","+predecessor;
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "update nodes", update);
    }


    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            BufferedReader bufferedReader;
            Log.e(TAG, "Server Started");
            int index = 0;

            try {
                while(true) {
                    Socket socket = serverSocket.accept();
                    Log.i(TAG, "socket created");

                    PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
                    bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String input = bufferedReader.readLine();

                    Log.i(TAG, input);
                    if (input != null) {
                        String[] arrayOfStrings = input.split(",");
                        if(arrayOfStrings[0].equals("new node join")) {
                            Log.i(TAG, "split occurred:" + arrayOfStrings[1] + " " + arrayOfStrings[2]);
                            Log.i(TAG,"New Node join request from "+arrayOfStrings[1]);

                            hashed.put(arrayOfStrings[2], arrayOfStrings[1]);
//
//                            aList.add(arrayOfStrings[1]);
//                            aList.add(arrayOfStrings[2]);
                            arrays.add(arrayOfStrings[2]);
                            Collections.sort(arrays);
                            Log.i(TAG, String.valueOf(arrays));

                            if(arrays.size() > 1) {
                                for (index = 0; index<arrays.size(); index++){
                                    if (index == 0) {
                                        avdId = hashed.get(arrays.get(index));
                                        successor = hashed.get(arrays.get(index + 1));
                                        predecessor = hashed.get(arrays.get(arrays.size() - 1));

                                        Log.i(TAG, avdId + " " + successorPort + " " + predecessorPort);
                                    } else if (index == arrays.size() - 1) {
                                        avdId = hashed.get(arrays.get(index));
                                        successor = hashed.get(arrays.get(0));
                                        predecessor = hashed.get(arrays.get(index - 1));

                                        Log.i(TAG, avdId + " " + successorPort + " " + predecessorPort);
                                    } else {
                                        avdId = hashed.get(arrays.get(index));
                                        successor = hashed.get(arrays.get(index + 1));
                                        predecessor = hashed.get(arrays.get(index - 1));

                                        Log.i(TAG, avdId + " " + successorPort + " " + predecessorPort);
                                    }
                                    if(!(avdId.equals(fNode))) {
                                        Log.i(TAG,"AVD ID " + avdId + "Successor " + successor + "Predecessor is " + predecessor);
                                        updateNodes();
                                    }
                                    else {
                                        successorPort = successor;
                                        predecessorPort = predecessor;
                                        Log.i(TAG, "successor port is:" + successorPort+ "predecessor port is:" + predecessorPort);
                                    }
                                }
                            }
                        }
                        else if(arrayOfStrings[0].contains("insert")) {
                            Log.i(TAG, "client part of insert");
                            ContentValues keyValueToInsert = new ContentValues();
                            keyValueToInsert.put(KEY_FIELD, arrayOfStrings[1]);
                            keyValueToInsert.put(VALUE_FIELD, arrayOfStrings[2]);

                            getContext().getContentResolver().insert(mUri, keyValueToInsert);
                        }
                        else if(arrayOfStrings[0].contains("update nodes")) {

                            successorPort = arrayOfStrings[1];
                            predecessorPort = arrayOfStrings[2];
                            arrays.add(genHash(successorPort));
                            arrays.add(genHash(predecessorPort));

                            Log.i(TAG,"Updated sp values at" + portStr +" successor is " + successorPort +  " predecessor is "+predecessorPort);
                        }
//                        else if(arrayOfStrings[0].contains("query")){
//                            Log.i(TAG, "inside server query");
//                            Log.i(TAG, "source port:" +arrayOfStrings[1]);
//                            Log.i(TAG, "selection contents:" + arrayOfStrings[3]);
//                            String sourcePort = arrayOfStrings[1];
//                            String selection = arrayOfStrings[3];
//                            query(mUri, null, String.valueOf(selection)+ ","+ sourcePort, null, null);
//                        }
//                        else if (arrayOfStrings[0].contains("result"))
//                        {
//                            Log.i(TAG, "routed query" + arrayOfStrings[1]);
//                            routedQuery = arrayOfStrings[1];
//                        }
//                        else if(arrayOfStrings[0].contains("star"))
//                        {
//                            Log.i(TAG, "star query part" + arrayOfStrings[1]);
//                            String received = arrayOfStrings[1];
//                            query(mUri, null, String.valueOf("@")+ ","+ received, null, null);
//                        }
//                        else if(arrayOfStrings[0].contains("response of asterisk"))
//                        {
//                            Log.i(TAG, "result of star query" + arrayOfStrings[2]);
//                            queryString = queryString + arrayOfStrings[2] + "-";
//                        }
//                        else
//                        {
//                            Log.i(TAG, "do nothing");
//                        }
                    }
                        bufferedReader.close();
                    socket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Couldn't connect with IO");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "mood hua, log daala");
                e.printStackTrace();
            }
            return null;
        }
   }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {

                String msgToSend = msgs[0];

                if(msgToSend.contains("new node join" + portStr)) {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(fPort));
                    Log.i(TAG,"Socket status to "+fPort +" is "+socket.isConnected());
                    Log.i(TAG, "socket created?" + socket);
                    try {
                        PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
                        String rcv = genHash(String.valueOf(portStr));
                        pw.println("new node join" + "," + portStr + "," + rcv);
                        pw.flush();
                        Log.e(TAG, "Sent message");
                        Log.i(TAG,"Sent join req for port " + fPort );
                    } catch (Exception e) {
                        Log.e(TAG, "aweyi exception");
                        e.printStackTrace();
                    }
                    socket.close();
                }
                else if(msgToSend.contains("update nodes"))
                {
                    String[] msg = msgs[1].split(",");
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(msg[0]) * 2);
                    PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
                    printWriter.println("update nodes" +"," + msg[1] + "," + msg[2]);
                    printWriter.flush();
                    socket.close();
                }
                else if(msgToSend.contains("insert"))
                {
                    Log.i(TAG,"Sending insert to successor" + msgs[3] + " successor is " + successorPort);
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(successorPort) * 2);
                    PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
                    printWriter.println("insert" +"," + msgs[3]);
                    printWriter.flush();
                    socket.close();
                }
//                else if(msgToSend.contains("query"));
//                {
////                    Log.i(TAG, "port:" + msgs[2]);
////                    Log.i(TAG, "current port:" + msgs[1]);
////                    Log.i(TAG, "first message parameter:" + msgs[0]);
////                    Log.i(TAG, "should contain the message:" + msgs[3]);
//                    String [] string = msgs[0].split(",");
//                    String succPort = string[0];
//                    Log.i(TAG, "what is present here? query part in client" +string[0]);
//                    Log.i(TAG, "succ port contains:" + succPort);
////                    Log.i(TAG, "what is present here?" +str[1]);
////                    Log.i(TAG, "what is present here?" +str[2]);
//                    Log.i(TAG, "Query request sent to"+ " "+ successorPort);
//                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(succPort));
//                    PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
//                    printWriter.println("query" + "," + msgs[1]);
//                    printWriter.flush();
//                    socket.close();
//                }
//                if(msgToSend.contains("result"))
//                {
//                    Log.i(TAG, "what is present here? result part in client" +msgs[1]);
//                    Log.i(TAG, "client part of query");
//                    String[] str = msgs[0].split(",");
//                    String sourcePort = str[1];
//                    Log.i(TAG, "what is present here? result on client side" +str[1]);
//                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(sourcePort));
//                    PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
//                    printWriter.println("result" +"," + msgs[1]);
//                    printWriter.flush();
//                    socket.close();
//                }
//                else if(msgToSend.contains("star"))
//                {
//                    Log.i(TAG, "client part of star query");
//                    Log.i(TAG, "what is present here? client star query" +msgs[1]);
//                    Log.i(TAG, "what is present here? client star query" +msgs[0]);
//                    String received = msgs[1];
//                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(received));
//                    PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
//                    printWriter.println("star" +"," + msgs[1]);
//                    printWriter.flush();
//                    socket.close();
//                }
//                else if(msgToSend.contains("resQue"))
//                {
//                    Log.i(TAG, "client part of star query result");
//                    Log.i(TAG, "what is present here? client star query result" +msgs[1]);
//                    String [] stringRec = msgs[0].split("@");
//                    String received = stringRec[1];
//                    Log.d("resQue", received);
//                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(received));
//                    PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
//                    printWriter.println("resQue" +"," + msgs[1]);
//                    printWriter.flush();
//                    socket.close();
//                }
//                else
//                {
//                    Log.i(TAG, "do nothing");
//                }
                } catch (UnknownHostException e) {
                Log.e(TAG, "host gaya tel lene");
                e.printStackTrace();
            } catch (IOException e) {
                Log.e(TAG, "IO bhi usske peeche gaya");
                e.printStackTrace();
            }
            return null;
        }
    }

}