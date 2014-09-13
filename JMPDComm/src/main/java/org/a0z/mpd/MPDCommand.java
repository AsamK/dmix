/*
 * Copyright (C) 2004 Felipe Gustavo de Almeida
 * Copyright (C) 2010-2014 The MPDroid Project
 *
 * All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice,this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.a0z.mpd;

import android.util.Log;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class MPDCommand {

    public static final int MIN_VOLUME = 0;

    public static final int MAX_VOLUME = 100;

    public static final String MPD_CMD_CLEARERROR = "clearerror";

    public static final String MPD_CMD_CLOSE = "close";

    public static final String MPD_CMD_COUNT = "count";

    public static final String MPD_CMD_CROSSFADE = "crossfade";

    public static final String MPD_CMD_FIND = "find";

    public static final String MPD_CMD_KILL = "kill";

    public static final String MPD_CMD_LIST_TAG = "list";

    public static final String MPD_CMD_LISTALL = "listall";

    public static final String MPD_CMD_LISTALLINFO = "listallinfo";

    public static final String MPD_CMD_LISTPLAYLISTS = "listplaylists";

    public static final String MPD_CMD_LSDIR = "lsinfo";

    public static final String MPD_CMD_GROUP = "group";

    public static final String MPD_CMD_NEXT = "next";

    public static final String MPD_CMD_PAUSE = "pause";

    public static final String MPD_CMD_PASSWORD = "password";

    public static final String MPD_CMD_PERMISSION = "permission";

    public static final String MPD_CMD_PLAY = "play";

    public static final String MPD_CMD_PLAY_ID = "playid";

    public static final String MPD_CMD_PREV = "previous";

    public static final String MPD_CMD_REFRESH = "update";

    public static final String MPD_CMD_REPEAT = "repeat";

    public static final String MPD_CMD_CONSUME = "consume";

    public static final String MPD_CMD_SINGLE = "single";

    public static final String MPD_CMD_RANDOM = "random";

    public static final String MPD_CMD_SEARCH = "search";

    public static final String MPD_CMD_SEEK = "seek";

    public static final String MPD_CMD_SEEK_ID = "seekid";

    public static final String MPD_CMD_STATISTICS = "stats";

    public static final String MPD_CMD_STATUS = "status";

    public static final String MPD_CMD_STOP = "stop";

    public static final String MPD_CMD_SET_VOLUME = "setvol";

    public static final String MPD_CMD_OUTPUTS = "outputs";

    public static final String MPD_CMD_OUTPUTENABLE = "enableoutput";

    public static final String MPD_CMD_OUTPUTDISABLE = "disableoutput";

    public static final String MPD_CMD_PLAYLIST_INFO = "listplaylistinfo";

    public static final String MPD_CMD_PLAYLIST_ADD = "playlistadd";

    public static final String MPD_CMD_PLAYLIST_MOVE = "playlistmove";

    public static final String MPD_CMD_PLAYLIST_DEL = "playlistdelete";

    private static final List<String> NON_RETRYABLE_COMMANDS = Arrays.asList(MPD_CMD_NEXT,
            MPD_CMD_PREV, MPD_CMD_PLAYLIST_ADD, MPD_CMD_PLAYLIST_MOVE, MPD_CMD_PLAYLIST_DEL);

    public static final String MPD_CMD_IDLE = "idle";

    public static final String MPD_CMD_PING = "ping";

    // deprecated commands
    public static final String MPD_CMD_VOLUME = "volume";

    /**
     * MPD default TCP port.
     */
    public static final int DEFAULT_MPD_PORT = 6600;

    public static final String MPD_FIND_ALBUM = "album";

    public static final String MPD_FIND_ARTIST = "artist";

    public static final String MPD_SEARCH_ALBUM = "album";

    public static final String MPD_SEARCH_ARTIST = "artist";

    public static final String MPD_SEARCH_FILENAME = "filename";

    public static final String MPD_SEARCH_TITLE = "title";

    public static final String MPD_SEARCH_GENRE = "genre";

    public static final String MPD_TAG_ALBUM = "album";

    public static final String MPD_TAG_ARTIST = "artist";

    public static final String MPD_TAG_ALBUM_ARTIST = "albumartist";

    public static final String MPD_TAG_GENRE = "genre";

    private static final String TAG = "MPDCommand";

    private static final boolean DEBUG = false;

    private static final Pattern QUOTATION_DELIMITER = Pattern.compile("\"");

    final String mCommand;

    private final String[] mArgs;

    public MPDCommand(final String command, final String... args) {
        super();
        mCommand = command;
        mArgs = args.clone();
    }

    public static boolean isRetryable(final String command) {
        return !NON_RETRYABLE_COMMANDS.contains(command);
    }

    public String toString() {
        final int argsLength = Arrays.toString(mArgs).length();
        final int approximateLength = argsLength + argsLength * (mCommand.length() + 10);
        final StringBuilder outBuf = new StringBuilder(approximateLength);
        outBuf.append(mCommand);
        for (String arg : mArgs) {
            if (arg == null) {
                continue;
            }
            arg = QUOTATION_DELIMITER.matcher(arg).replaceAll("\\\\\"");
            outBuf.append(" \"");
            outBuf.append(arg);
            outBuf.append('"');
        }
        outBuf.append('\n');
        final String outString = outBuf.toString();
        if (DEBUG) {
            final String safeCommand;
            if (outString.startsWith(MPD_CMD_PASSWORD)) {
                safeCommand = "password **censored**";
            } else {
                safeCommand = outString;
            }
            Log.d("JMPDComm", "MPD command : " + safeCommand);
        }
        return outString;
    }
}
