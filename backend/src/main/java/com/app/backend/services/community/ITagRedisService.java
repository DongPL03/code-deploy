package com.app.backend.services.community;

import com.app.backend.responses.community.TagResponse;
import java.util.List;

public interface ITagRedisService {
    /**
     * Get all public tags from cache
     * @return List of TagResponse or null if cache miss
     */
    List<TagResponse> getAllTags();

    /**
     * Save all public tags to cache
     */
    void saveAllTags(List<TagResponse> tags);

    /**
     * Clear tag cache (call when tags are modified)
     */
    void clearTagCache();
}
