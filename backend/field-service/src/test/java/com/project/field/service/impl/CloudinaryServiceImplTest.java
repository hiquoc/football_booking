package com.project.field.service.impl;

import com.cloudinary.Api;
import com.cloudinary.Cloudinary;
import com.project.field.exceptions.ImageStorageException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CloudinaryServiceImplTest {

    @Test
    void deleteImagesUsesCloudinaryBatchApiOnce() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Api api = mock(Api.class);
        when(cloudinary.api()).thenReturn(api);
        CloudinaryServiceImpl service = new CloudinaryServiceImpl(cloudinary);

        service.deleteImages(List.of(
                "https://res.cloudinary.com/demo/image/upload/v1/first.jpg",
                "https://res.cloudinary.com/demo/image/upload/v1/second.png"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<String>> idsCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(api).deleteResources(idsCaptor.capture(), anyMap());
        assertThat(idsCaptor.getValue()).containsExactly("first", "second");
    }

    @Test
    void deleteImagesWrapsProviderFailureInDomainException() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Api api = mock(Api.class);
        when(cloudinary.api()).thenReturn(api);
        when(api.deleteResources(org.mockito.ArgumentMatchers.<Iterable<String>>any(), anyMap()))
                .thenThrow(new IOException("provider unavailable"));
        CloudinaryServiceImpl service = new CloudinaryServiceImpl(cloudinary);

        assertThatThrownBy(() -> service.deleteImages(List.of(
                "https://res.cloudinary.com/demo/image/upload/v1/first.jpg")))
                .isInstanceOf(ImageStorageException.class)
                .hasMessage("Image storage provider failed to delete uploaded images")
                .hasCauseInstanceOf(IOException.class);
    }
}
