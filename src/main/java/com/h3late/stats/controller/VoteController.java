package com.h3late.stats.controller;

import com.h3late.stats.entity.LeaderboardEntry;
import com.h3late.stats.entity.Vote;
import com.h3late.stats.service.VoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/votes")
public class VoteController {

    private final VoteService voteService;

    public VoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    @PostMapping("/{videoId}")
    public ResponseEntity<Vote> castVote(
            @PathVariable String videoId,
            @RequestBody Vote voteRequest) {
        return ResponseEntity.ok(voteService.castVote(videoId, voteRequest));
    }


    @GetMapping("/leaderboard/latest")
    public ResponseEntity<List<LeaderboardEntry>> getLatestLeaderboard() {
        // This pulls from the SQL view we created
        return ResponseEntity.ok(voteService.getLatestLeaderboard());
    }
}