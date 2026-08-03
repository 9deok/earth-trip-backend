package com.earthtrip.platform.adapter.out.persistence.file;

import com.earthtrip.platform.application.port.out.FileStorePort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class FilePersistenceAdapter implements FileStorePort {

    private final FileJpaRepository files;
    private final UploadSessionJpaRepository uploads;
    private final FileLinkJpaRepository links;

    FilePersistenceAdapter(
        FileJpaRepository files,
        UploadSessionJpaRepository uploads,
        FileLinkJpaRepository links
    ) {
        this.files = files;
        this.uploads = uploads;
        this.links = links;
    }

    @Override
    public Optional<FileRecord> file(UUID id) {
        return files.findById(id.toString()).map(FileJpaEntity::toRecord);
    }

    @Override
    public FileRecord saveFile(FileRecord record) {
        FileJpaEntity entity = files.findById(record.id().toString())
            .map(current -> {
                current.apply(record);
                return current;
            })
            .orElseGet(() -> new FileJpaEntity(record));
        return files.saveAndFlush(entity).toRecord();
    }

    @Override
    public Optional<UploadRecord> upload(UUID id) {
        return uploads.findById(id.toString()).map(UploadSessionJpaEntity::toRecord);
    }

    @Override
    public UploadRecord saveUpload(UploadRecord record) {
        UploadSessionJpaEntity entity = uploads.findById(record.id().toString())
            .map(current -> {
                current.apply(record);
                return current;
            })
            .orElseGet(() -> new UploadSessionJpaEntity(record));
        return uploads.save(entity).toRecord();
    }

    @Override
    public List<LinkRecord> links(UUID fileId) {
        return links.findAllByFileIdOrderByLinkedAtAsc(fileId.toString()).stream()
            .map(FileLinkJpaEntity::toRecord)
            .toList();
    }

    @Override
    public List<LinkRecord> linksForTrip(UUID tripId) {
        return links.findAllByTripIdOrderByLinkedAtAsc(tripId.toString()).stream()
            .map(FileLinkJpaEntity::toRecord)
            .toList();
    }

    @Override
    public List<LinkRecord> links(UUID tripId, String resourceType, UUID resourceId) {
        return links.findAllByTripIdAndResourceTypeAndResourceIdOrderByLinkedAtAsc(
                tripId.toString(), resourceType, resourceId.toString()
            ).stream()
            .map(FileLinkJpaEntity::toRecord)
            .toList();
    }

    @Override
    public Optional<LinkRecord> link(UUID id) {
        return links.findById(id.toString()).map(FileLinkJpaEntity::toRecord);
    }

    @Override
    public LinkRecord saveLink(LinkRecord record) {
        return links.save(new FileLinkJpaEntity(record)).toRecord();
    }

    @Override
    public void deleteLink(UUID id) {
        links.deleteById(id.toString());
    }
}
