package org.apache.ozone.snapshot;

import com.sun.tools.javac.util.List;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.hadoop.hdds.client.ReplicationConfig;
import org.apache.hadoop.hdds.client.ReplicationFactor;
import org.apache.hadoop.hdds.client.ReplicationType;
import org.apache.hadoop.hdds.utils.db.Table;
import org.apache.hadoop.ozone.OzoneAcl;
import org.apache.hadoop.ozone.om.helpers.OmKeyInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;

public class OMKeyTableWriter {

    OMDB omdb;
    private Table<String, OmKeyInfo> keyTable;

    private String volume;
    private String bucket;

    public OMKeyTableWriter(OMDB omdb) throws IOException {
        this.omdb = omdb;
        this.keyTable = omdb.getKeyTable();
        this.volume = "movies";
        this.bucket = "hollywood";
    }



    public void generate() throws Exception {
        TarArchiveInputStream tarInput = new TarArchiveInputStream(
                new GzipCompressorInputStream(
                        getClass().getClassLoader()
                                .getResourceAsStream("title.tar.gz")));

        TarArchiveEntry currentEntry = tarInput.getNextTarEntry();
        BufferedReader br;
        while (currentEntry != null) {
            br = new BufferedReader(new InputStreamReader(tarInput));
            final String keyLocation = "/" + volume + "/" + bucket + "/";
            String title;
            while ((title = br.readLine()) != null) {
                keyTable.put(keyLocation + title, getKeyInfo(title));
            }
            currentEntry = tarInput.getNextTarEntry();
        }
    }

    public void generateRandom(int numKeys) throws Exception {
        for (int i = 0; i < numKeys; i++) {
            String key =  "/" + volume + "/" + bucket + "/" + UUID.randomUUID();
            keyTable.put(key, getKeyInfo(key));
            if(i % 1_000_000 == 0) {
                System.out.println("Generated " + i + " keys");
            }
        }
    }

    private OmKeyInfo getKeyInfo(final String key) {
        final OmKeyInfo.Builder builder = new OmKeyInfo.Builder();
        builder.setVolumeName(volume)
                .setBucketName(bucket)
                .setKeyName(key)
                .setDataSize(10000000)
                .setCreationTime(System.currentTimeMillis())
                .setModificationTime(System.currentTimeMillis())
                .setReplicationConfig(ReplicationConfig.fromTypeAndFactor(
                        ReplicationType.RATIS, ReplicationFactor.THREE))
                .setAcls(List.of(OzoneAcl.parseAcl("user:imdb:rw")));
        return builder.build();
    }

    public void close() throws Exception {
        keyTable.close();
        omdb.close();
    }
}