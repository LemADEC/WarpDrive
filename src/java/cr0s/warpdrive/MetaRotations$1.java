package cr0s.warpdrive;

import java.io.File;
import java.io.FilenameFilter;

class MetaRotations$1 implements FilenameFilter
{
    final MetaRotations this$0;

    MetaRotations$1(MetaRotations var1)
    {
        this.this$0 = var1;
    }

    @Override
	public boolean accept(File f, String name)
    {
        return name.endsWith(".mrot");
    }
}
