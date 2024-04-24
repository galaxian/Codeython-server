package clofi.codeython.problem.judge.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import clofi.codeython.member.domain.Member;
import clofi.codeython.member.repository.MemberRepository;
import clofi.codeython.problem.core.domain.LanguageType;
import clofi.codeython.problem.core.domain.Problem;
import clofi.codeython.problem.core.domain.Record;
import clofi.codeython.problem.core.domain.Testcase;
import clofi.codeython.problem.core.repository.ProblemRepository;
import clofi.codeython.problem.core.repository.RecordRepository;
import clofi.codeython.problem.core.repository.TestcaseRepository;
import clofi.codeython.problem.judge.domain.ResultCalculator;
import clofi.codeython.problem.judge.domain.creator.ExecutionFileCreator;
import clofi.codeython.problem.judge.dto.request.ExecutionRequest;
import clofi.codeython.problem.judge.dto.request.SubmitRequest;
import clofi.codeython.problem.judge.dto.response.ExecutionResponse;
import clofi.codeython.problem.judge.dto.response.SubmitResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
@Transactional(readOnly = true)
@Service
public class JudgeService {
    private final Map<String, ExecutionFileCreator> executionFileCreatorMap;
    private final ResultCalculator resultCalculator;
    private final ProblemRepository problemRepository;
    private final TestcaseRepository testcaseRepository;
    private final RecordRepository recordRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public SubmitResponse submit(SubmitRequest submitRequest, Long problemNo, Member tokenMember) {
        Member member = memberRepository.findByUsername(tokenMember.getUsername());
        Problem problem = problemRepository.findById(problemNo)
            .orElseThrow(() -> new IllegalArgumentException("없는 문제 번호입니다."));

        String route = UUID.randomUUID() + "/";
        createFolder(route);
        try {
            ExecutionFileCreator executionFileCreator = executionFileCreatorMap
                .get(LanguageType.getCreatorName(submitRequest.language()));

            executionFileCreator.create(problem.getType(), submitRequest.code(), route);

            List<Testcase> testcases = testcaseRepository.findAllByProblemProblemNo(problemNo);

            int accuracy = resultCalculator.judge(route, submitRequest.language(), testcases);
            if (submitRequest.roomId() == null) {
                recordRepository.save(
                    new Record(submitRequest.code(), member, problem, submitRequest.language().toUpperCase(),
                        accuracy, null, null));

            }
            return new SubmitResponse(accuracy, null, null);
        } finally {
            cleanup(route);
        }
    }

    private void createFolder(String route) {
        try {
            Files.createDirectory(Path.of(route));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void cleanup(String route) {
        try {
            FileUtils.deleteDirectory(new File(route));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public List<ExecutionResponse> execution(ExecutionRequest executionRequest, long problemNo) {
        Problem problem = problemRepository.findById(problemNo)
            .orElseThrow(() -> new IllegalArgumentException("없는 문제 번호입니다."));

        String route = UUID.randomUUID() + "/";
        createFolder(route);
        try {
            ExecutionFileCreator executionFileCreator = executionFileCreatorMap
                .get(LanguageType.getCreatorName(executionRequest.language()));

            executionFileCreator.create(problem.getType(), executionRequest.code(), route);

            List<Testcase> testcases = testcaseRepository.findAllByProblemProblemNo(problemNo);

            return resultCalculator.execution(route, executionRequest.language(), testcases);
        } finally {
            cleanup(route);
        }
    }
}
