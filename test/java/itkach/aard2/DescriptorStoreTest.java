package itkach.aard2;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;

@RunWith(RobolectricTestRunner.class)
@Config(application = android.app.Application.class, sdk = 28)
public class DescriptorStoreTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void savingExistingDescriptorReplacesItWithoutLeavingTempFiles() throws Exception {
        File dir = tmp.newFolder("descriptors");
        DescriptorStore<BaseDescriptor> store = new DescriptorStore<>(new ObjectMapper(), dir);
        BaseDescriptor descriptor = new BaseDescriptor();
        descriptor.id = "item";
        descriptor.lastAccess = 1;
        store.save(descriptor);

        descriptor.lastAccess = 2;
        store.save(descriptor);

        assertEquals(1, store.load(BaseDescriptor.class).size());
        assertEquals(2, store.load(BaseDescriptor.class).get(0).lastAccess);
        assertEquals(1, dir.listFiles().length);
    }

    @Test
    @Config(sdk = 23)
    public void savingNewDescriptorWorksOnLegacyAndroid() throws Exception {
        File dir = tmp.newFolder("legacy-descriptors");
        DescriptorStore<BaseDescriptor> store = new DescriptorStore<>(new ObjectMapper(), dir);
        BaseDescriptor descriptor = new BaseDescriptor();
        descriptor.id = "item";

        store.save(descriptor);

        assertEquals(1, store.load(BaseDescriptor.class).size());
    }
}
