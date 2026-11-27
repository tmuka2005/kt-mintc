---
name: benchmark
description: Measure parser performance with the JMH benchmarks in benchmark/. Use when asked to benchmark or profile, or to check whether a change to libs/parser, libs/unicode or benchmark/ made parsing faster or slower.
---

# Benchmark

Run every command from the repository root. `$S` below is a directory you
create in the scratchpad for this measurement, such as `$scratchpad/bench`;
write it out as an absolute path in each command.

## Suites

All in `benchmark/src/commonMain/kotlin/com/t_mukai/mint/benchmark/`.
Inputs come from `BenchmarkInput.kt`.

| Class | Methods | `@Param` | Run it when changing |
|---|---|---|---|
| `LibraryBenchmark` | `myParser`, `betterParse`, `parsus` | `shape`: flat, mixed, nested | `libs/parser`, its own files |
| `ClusterBenchmark` | `parse` | `digits`: ascii, fullwidth, decorated | `libs/parser`, `libs/unicode`, its own files |
| `ParserBenchmark` | `plain`, `hybrid`, `alwaysBounce` | `shape`: flat, mixed, nested | its own files only |

`ParserBenchmark` is a standalone experiment on suspend-based recursion and
does not use `libs:parser`.

All suites measure average time per operation, so lower is better.

The `betterParse` and `parsus` cases do not depend on this repository, so a
change to `libs/parser` uses `'LibraryBenchmark.myParser|ClusterBenchmark'`
and a change to `libs/unicode` uses `'ClusterBenchmark'`.

Speed of non-ASCII input (`fullwidth`, `decorated`) is not a goal for now.
Report it, but do not optimize for it.

## Measuring

Rebuild the jar after every source change, including after switching
revisions:

```bash
./gradlew :benchmark:jvmBenchmarkJar -q
```

Smoke test the selected suites first. It runs each `setUp`, whose checks
fail the run when the parsers disagree:

```bash
java -jar benchmark/build/benchmarks/jvm/jars/benchmark-jvm-jmh-JMH.jar '<regex>' -wi 0 -i 1 -r 200ms -f 1
```

Then measure with the standard configuration, writing the result straight to
`$S`:

```bash
java -jar benchmark/build/benchmarks/jvm/jars/benchmark-jvm-jmh-JMH.jar '<regex>' -wi 5 -i 5 -r 1s -w 1s -f 1 -rf json -rff $S/<name>.json > $S/<name>.log 2>&1
```

`<regex>` matches `Class.method`, for example
`'LibraryBenchmark|ClusterBenchmark'`. Add `-p shape=mixed` to fix a
parameter. Each case takes about 10 s. Run the measurement in the background
and keep the machine otherwise idle.

`./gradlew :benchmark:benchmark` runs all 21 cases with the same
configuration, but writes into a timestamped directory under
`benchmark/build/reports/benchmarks/main/`. Prefer the jar.

## Comparing a change

Measure the baseline and the change back to back with the same `<regex>`:

1. Find the change: `@` if it has a diff, otherwise `@-`. Confirm with
   `jj diff --stat -r <rev>` and record its change id with
   `jj log -r <rev> --no-graph -T change_id`.
2. `jj new <change id>-`, then rebuild the jar, smoke test and measure into
   `$S/baseline.json`.
3. `jj new <change id>`, then rebuild the jar, smoke test and measure into
   `$S/current.json`. The working copy stays on this empty change; the
   change itself is untouched.
4. Summarize:

   ```bash
   uv run --no-project python .claude/skills/benchmark/summarize.py $S/baseline.json $S/current.json
   ```

   It prints the JDK, then one table per suite, with methods as rows and
   parameters as columns. Each cell is `baseline ± error → current ± error (ratio)`, where
   the ratio is current / baseline in the unit shown in the heading (`us/op`
   is lower-is-better). A `*` marks a difference larger than both errors
   combined; any other difference is noise. A single-iteration run has no
   error and never gets a `*`. With only one file it prints the scores alone.

When a case the change targets shows no `*`, rerun both sides with `-i 10`
before concluding that it did not change.

## Profiling

To explain a case marked `*`, profile it on both revisions with the stack
profiler and compare the top frames:

```bash
java -jar benchmark/build/benchmarks/jvm/jars/benchmark-jvm-jmh-JMH.jar '<Class.method>' -p <param>=<value> -wi 3 -i 3 -r 1s -f 1 -prof 'stack:lines=1;top=12;detailLine=false'
```

## Reporting

Answer in the conversation. Start with the machine
(`sysctl -n machdep.cpu.brand_string`), then paste the output of
`summarize.py`, then the analysis: which cases changed beyond noise, by how
much, and why if profiling showed it. If the cases disagree, say so per case
instead of giving one verdict.
