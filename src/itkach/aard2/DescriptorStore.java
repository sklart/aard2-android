package itkach.aard2;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

class DescriptorStore<T extends BaseDescriptor> {

    static final String TAG = DescriptorStore.class.getSimpleName();

    // Suffix for the temporary file save() writes before atomically renaming it
    // into place. load() skips these so a reader never parses a half-written one.
    private static final String TMP_SUFFIX = ".tmp";

    private File         dir;
    private ObjectMapper mapper;

    DescriptorStore(ObjectMapper mapper, File dir) {
        this.dir = dir;
        this.mapper = mapper;
    }

    List<T> load(Class<T> type) {
        List<T> result = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                // A save() in progress (or left behind by a crash mid-save); not a
                // descriptor. Skipping it keeps the delete-on-parse-failure below
                // from ever deleting a file that's simply being written.
                if (f.getName().endsWith(TMP_SUFFIX)) {
                    continue;
                }
                try {
                    T sd = mapper.readValue(f, type);
                    result.add(sd);
                } catch (Exception e) {
                    String path = f.getAbsolutePath();
                    Log.w(TAG, String.format("Loading data from file %s failed", path), e);
                    boolean deleted = f.delete();
                    Log.w(TAG, String.format("Attempt to delete corrupted file %s succeeded? %s",
                            path, deleted));
                }
            }
        }
        return result;
    }

    void save(List<T> lst) {
        for (T item : lst) {
            save(item);
        }
    }

    void save(T item) {
        if (item.id == null) {
            Log.d(getClass().getName(), "Can't save item without id");
            return;
        }
        // Write to a temp file and rename it into place, so a concurrent reader
        // (the background load at startup) never sees a partially-written file -
        // which load() would treat as corrupt and delete. The temp lives in the
        // same directory as the target so replacing it can be atomic.
        File target = new File(dir, item.id);
        File tmp = null;
        try {
            tmp = File.createTempFile(item.id + "-", TMP_SUFFIX, dir);
            mapper.writeValue(tmp, item);
            replaceAtomically(tmp, target);
            tmp = null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (tmp != null) {
                tmp.delete();
            }
        }
    }

    private static void replaceAtomically(File source, File target) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            replaceAtomicallyO(source, target);
        } else if (!source.renameTo(target)) {
            throw new IOException(String.format("Failed to rename %s to %s", source, target));
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private static void replaceAtomicallyO(File source, File target) throws IOException {
        Files.move(source.toPath(), target.toPath(),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    boolean delete(String itemId) {
        if (itemId == null) {
            return false;
        }
        return new File(dir, itemId).delete();
    }

}
