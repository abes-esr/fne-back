package fr.fne.core.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

// Test YML file for the configuration properties ( Not use for project )
public class YamlFileApplication implements PropertySourceFactory {

    @Override
    public PropertySource<?> createPropertySource(@Nullable String name, EncodedResource resource) throws IOException {
        Properties loadedProperties = this.loadYamlIntoProperties(resource.getResource());
        return new PropertiesPropertySource((StringUtils.isNotBlank(name)) ? name : Objects.requireNonNull(resource.getResource().getFilename()), loadedProperties);
    }

    private Properties loadYamlIntoProperties(Resource resource) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(resource);
        factory.afterPropertiesSet();
        return factory.getObject();
    }
}
