package guru.nicks.log;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.springframework.beans.BeanUtils;

import java.util.Map;
import java.util.TreeMap;

/**
 * A custom {@link PatternLayout} that sorts MDC properties case-insensitively before formatting log events. This
 * ensures, during local development, consistent and readable ordering of MDC key-value pairs in log output regardless
 * of insertion order. In production, the log format is JSON, therefore this layout isn't needed.
 *
 * @see #doLayout(ILoggingEvent)
 */
public class SortedMdcLayout extends PatternLayout {

    /**
     * It's not allowed to change MDCPropertyMap in the original event (an exception is thrown), therefore:
     * <ol>
     *  <li>create empty event</li>
     *  <li>clone all properties but MDCPropertyMap using reflection (which is <b>slow</b>, but this layout is not
     *      used in Docker i.e. in production)</li>
     *  <li>pass sorted map to MDCPropertyMap setter (it works because the property is null)</li>
     *  <li>pass cloned event to parent class</li>
     * </ol>
     */
    @Override
    public String doLayout(ILoggingEvent event) {
        Map<String, String> mdcMap = event.getMDCPropertyMap();

        // nothing to do
        if ((mdcMap == null) || mdcMap.isEmpty()) {
            return super.doLayout(event);
        }

        LoggingEvent clonedEvent = new LoggingEvent();
        BeanUtils.copyProperties(event, clonedEvent, "MDCPropertyMap");

        // Theoretically, the sorted map can be cached because MDC isn't changed frequently within, for example, the
        // same HTTP request's thread (emitting dozens of log messages). But this would require thread safety (i.e.
        // synchronization) of all methods accessing the map, which reduces parallelism.
        var sortedMdcMap = new TreeMap<String, String>(String::compareToIgnoreCase);
        sortedMdcMap.putAll(mdcMap);
        clonedEvent.setMDCPropertyMap(sortedMdcMap);

        // delegate to parent class to perform the actual layout
        return super.doLayout(clonedEvent);
    }

}
