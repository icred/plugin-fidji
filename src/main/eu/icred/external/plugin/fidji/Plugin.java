/**
 * 
 */
package eu.icred.external.plugin.fidji;

import eu.icred.external.plugin.fidji.read.Reader;
import eu.icred.external.plugin.fidji.write.Writer;
import eu.icred.plugin.IPlugin;
import eu.icred.plugin.worker.input.IImportWorker;
import eu.icred.plugin.worker.output.IExportWorker;

/**
 * @author phoudek
 *
 */
public class Plugin implements IPlugin {

    /* (non-Javadoc)
     * @see eu.icred.plugin.IPlugin#isModelVersionSupported(java.lang.String)
     */
    @Override
    public boolean isModelVersionSupported(String version) {
        return version.startsWith("1-0.6.");
    }

    /* (non-Javadoc)
     * @see eu.icred.plugin.IPlugin#getPluginId()
     */
    @Override
    public String getPluginId() {
        return "fidji";
    }

    /* (non-Javadoc)
     * @see eu.icred.plugin.IPlugin#getPluginVersion()
     */
    @Override
    public String getPluginVersion() {
        return "0.1a";
    }

    /* (non-Javadoc)
     * @see eu.icred.plugin.IPlugin#getPluginName()
     */
    @Override
    public String getPluginName() {
        return "FIDJI-Plugin";
    }

    /* (non-Javadoc)
     * @see eu.icred.plugin.IPlugin#getImportPlugin()
     */
    @Override
    public IImportWorker getImportPlugin() {
        return new Reader();
    }

    /* (non-Javadoc)
     * @see eu.icred.plugin.IPlugin#getExportPlugin()
     */
    @Override
    public IExportWorker getExportPlugin() {
        return new Writer();
    }

}
