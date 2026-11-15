package com.bingbang.share;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.bingbang.session.Session;
import com.bingbang.share.ShareService.ShareDto;
import com.bingbang.share.ShareService.SharedViewDto;

import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api")
public class ShareController {

    private final ShareService shares;

    public ShareController(ShareService shares) {
        this.shares = shares;
    }

    @PostMapping("/shares")
    @ResponseStatus(HttpStatus.CREATED)
    public ShareDto create(Session session, @RequestBody(required = false) CreateShareRequest request) {
        return shares.create(session.walletId(), request == null ? null : request.listIds());
    }

    @GetMapping("/shares")
    public List<ShareDto> list(Session session) {
        return shares.list(session.walletId());
    }

    @DeleteMapping("/shares/{shareId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(Session session, @PathVariable String shareId) {
        shares.revoke(session.walletId(), shareId);
    }

    /** Public and read-only: the only endpoint a share link gives access to. */
    @GetMapping("/shared/{shareId}")
    public SharedViewDto view(@PathVariable String shareId) {
        return shares.view(shareId);
    }

    public record CreateShareRequest(@Size(max = 200) List<Long> listIds) {
    }
}
