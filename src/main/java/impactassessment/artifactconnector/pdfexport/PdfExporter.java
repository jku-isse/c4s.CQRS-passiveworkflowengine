package impactassessment.artifactconnector.pdfexport;

import com.jamasoftware.services.restclient.exception.RestClientException;
import com.jamasoftware.services.restclient.jamadomain.fields.JamaField;
import com.jamasoftware.services.restclient.jamadomain.fields.PickListField;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaItemType;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.PickList;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.PickListOption;
import impactassessment.artifactconnector.jama.IJamaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PdfExporter {

    private final IJamaService jamaService;

    private int lineCount = 0;
    private final int MAX_LINES = 50;
    private PDPageContentStream contentStream;
    private PDDocument doc;

    private Collection<JamaItemType> jamaItemTypes;
    private Collection<PickList> pickLists;
    private Collection<PickListOption> pickListOptions;

    public void generate(String artifactType) throws IOException {
        if (artifactType.equals("IJamaArtifact")) {
            fetchJamaData();
            generateJama();
        } else {
            log.error("Artifact type {} not supported!", artifactType);
        }
    }

    private void fetchJamaData() {
        jamaItemTypes = jamaService.fetchAllJamaItemTypes();
        pickLists = jamaService.fetchAllPickLists();
        pickListOptions = jamaService.fetchAllPickListOptions();
    }

    private void generateJama() throws IOException {
        doc = new PDDocument();

        // Jama Item Types
        newPage();
        writeLn("IJamaArtifact Type Description");
        contentStream.newLine();
        writeLn("Jama Item Types:");
        for (JamaItemType type : jamaItemTypes) {
            String s = type.getDisplay()+" (Key="+type.getTypeKey()+", ID="+type.getId()+")";
            addBulletPointWithLineBreak(s);
            addField(type);
        }

        // Pick Lists
        newPage();
        writeLn("Pick Lists:");
        for (PickList pickList : pickLists) {
            String s = pickList.getName()+" (ID="+pickList.getId()+"): "+pickList.getDescription();
            addBulletPointWithLineBreak(s);
        }

        // Pick List Options
        newPage();
        writeLn("Pick List Options:");
        for (PickListOption option : pickListOptions) {
            String s = option.getName()+" (ID="+option.getId()+"): "+option.getDescription();
            addBulletPointWithLineBreak(s);
        }

        contentStream.endText();
        contentStream.close();
        doc.save("pdfs/IJamaArtifact.pdf");
        doc.close();
    }

    private void writeLn(String s) throws IOException {
        contentStream.showText(s);
        contentStream.newLine();
        lineCount++;
        if (lineCount >= MAX_LINES) {
            newPage();
        }
    }

    private void newPage() throws IOException {
        lineCount = 0;
        if (contentStream != null) {
            contentStream.endText();
            contentStream.close();
        }
        PDPage page = new PDPage();
        doc.addPage(page);
        contentStream = new PDPageContentStream(doc, page);
        contentStream.setFont(PDType1Font.TIMES_ROMAN, 12);
        contentStream.beginText();
        contentStream.newLineAtOffset(25, 750);
        contentStream.setLeading(14.5f);
    }

    private void addBulletPointWithLineBreak(String s) throws IOException {
        int lineLength = 110;
        String offset = "   ";
        String text = offset + s;
        contentStream.showText("\u2022");
        if (text.length() > 2*lineLength) {
            writeLn(text.substring(0,lineLength));
            writeLn(offset + text.substring(lineLength, 2*lineLength));
            writeLn(offset + text.substring(2*lineLength));
        } else if (text.length() > lineLength) {
            writeLn(text.substring(0,lineLength));
            writeLn(offset + text.substring(lineLength));
        } else {
            writeLn(text);
        }
    }

    private void addField(JamaItemType type) throws IOException {
        for (JamaField field : type.getFields()) {
            contentStream.showText("      \u2022");
            if (field instanceof PickListField) {
                PickListField pickListField = (PickListField) field;
                try {
                    String options = pickListField.getPickList().getOptions().stream().map(PickListOption::getName).collect(Collectors.joining(", "));
                    if (options.length() > 0) {
                        writeLn("   " + field.getLabel() + ": " + field.getClass().getSimpleName() + " ("+pickListField.getPickList().getName()+": "+options+")");
                    } else {
                        writeLn("   " + field.getLabel() + ": " + field.getClass().getSimpleName() + " ("+pickListField.getPickList().getName()+": options coudn't be fetched)");
                    }
                } catch (RestClientException e) {
                    writeLn("   " + field.getLabel() + ": " + field.getClass().getSimpleName() + " ("+pickListField.getPickList().getName()+": options coudn't be fetched)");
                }

            } else {
                try {
                    writeLn("   " + field.getLabel() + ": " + field.getClass().getSimpleName());
                } catch (IllegalArgumentException e) { // prevent:  U+0009 ('controlHT') is not available in this font Times-Roman encoding: WinAnsiEncoding
                    writeLn("   " + " -");
                }
            }
        }
    }
}
